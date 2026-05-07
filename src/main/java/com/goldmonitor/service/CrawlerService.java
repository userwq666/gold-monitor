package com.goldmonitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goldmonitor.model.GoldPrice;
import com.goldmonitor.repository.GoldPriceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CrawlerService {

    private static final Logger log = LoggerFactory.getLogger(CrawlerService.class);

    private final GoldPriceRepository goldPriceRepository;
    private final ObjectMapper objectMapper;

    public CrawlerService(GoldPriceRepository goldPriceRepository) {
        this.goldPriceRepository = goldPriceRepository;
        this.objectMapper = new ObjectMapper();
    }

    public List<GoldPrice> fetchPrices() {
        List<GoldPrice> saved = new ArrayList<>();

        try {
            log.info("抓取实时金价 (上海黄金交易所)...");
            JsonNode sge = callPython();
            if (sge != null && sge.has("price") && !sge.has("error")) {
                GoldPrice record = new GoldPrice();
                record.setPrice(new BigDecimal(sge.get("price").asText()));
                record.setSource("上海黄金交易所 Au99.99");
                record.setUnit("元/克");
                record.setFetchTime(LocalDateTime.now());
                saved.add(goldPriceRepository.save(record));
                log.info("SGE实时价: {} 元/克", record.getPrice());
            } else {
                log.warn("SGE无数据: {}", sge);
            }
        } catch (Exception e) {
            log.warn("SGE抓取失败: {}", e.getMessage());
        }

        try {
            log.info("抓取银行金条价 (xxapi.cn)...");
            JsonNode data = fetchJson("https://v2.xxapi.cn/api/goldprice");
            if (data != null) {
                JsonNode bars = data.path("bank_gold_bar_price");
                if (bars.isArray()) {
                    int count = 0;
                    for (JsonNode bar : bars) {
                        String bank = bar.get("bank").asText();
                        BigDecimal price = new BigDecimal(bar.get("price").asText());
                        if (price.compareTo(BigDecimal.ZERO) <= 0) continue;
                        GoldPrice record = new GoldPrice();
                        record.setPrice(price);
                        record.setSource(bank);
                        record.setUnit("元/克");
                        record.setFetchTime(LocalDateTime.now());
                        saved.add(goldPriceRepository.save(record));
                        count++;
                    }
                    log.info("银行金条: {} 条", count);
                }
            } else {
                log.warn("xxapi无数据");
            }
        } catch (Exception e) {
            log.warn("xxapi抓取失败: {}", e.getMessage());
        }

        return saved;
    }

    private JsonNode callPython() throws Exception {
        String baseDir = System.getProperty("user.dir");
        java.io.File scriptFile = new java.io.File(baseDir, "fetch_sge.py");
        if (!scriptFile.exists()) {
            scriptFile = new java.io.File(baseDir, "../fetch_sge.py");
        }
        log.info("Python脚本路径: {}", scriptFile.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder("python", scriptFile.getAbsolutePath());
        pb.redirectErrorStream(true);

        Process proc = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }
        int exitCode = proc.waitFor();
        String json = output.toString().trim();
        //log.info("Python exit={} output={}", exitCode, json.length() > 0 ? json.substring(0, Math.min(200, json.length())) : "(empty)");
        if (json.isEmpty()) return null;
        return objectMapper.readTree(json);
    }

    private JsonNode fetchJson(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        if (conn.getResponseCode() != 200) return null;

        StringBuilder body = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) body.append(line);
        }
        JsonNode root = objectMapper.readTree(body.toString());
        if (root.has("code") && root.get("code").asInt() != 200) return null;
        return root.path("data");
    }

    public Map<String, Object> fetchAllQuotes() {
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> allBars = new ArrayList<>();
        Map<String, Object> sgeEntry = new HashMap<>();
        sgeEntry.put("bank", "上海黄金交易所 Au99.99");
        sgeEntry.put("price", goldPriceRepository
                .findTopBySourceAndFetchTimeAfterOrderByFetchTimeDesc("上海黄金交易所 Au99.99", LocalDateTime.now().minusHours(24))
                .map(p -> p.getPrice().toString()).orElse("--"));
        allBars.add(sgeEntry);

        try {
            JsonNode data = fetchJson("https://v2.xxapi.cn/api/goldprice");
            if (data != null) {
                JsonNode bars = data.path("bank_gold_bar_price");
                if (bars.isArray()) {
                    for (JsonNode bar : bars) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("bank", bar.get("bank").asText());
                        m.put("price", bar.get("price").asText());
                        allBars.add(m);
                    }
                }
                result.put("recycle", data.get("gold_recycle_price"));
                result.put("jewelry", data.get("precious_metal_price"));
            }
        } catch (Exception e) {
            log.warn("xxapi获取失败: {}", e.getMessage());
        }

        result.put("bank_bars", allBars);
        if (!result.containsKey("recycle")) result.put("recycle", Collections.emptyList());
        if (!result.containsKey("jewelry")) result.put("jewelry", Collections.emptyList());
        return result;
    }

    public List<GoldPrice> getHistory(LocalDateTime since) {
        return goldPriceRepository.findByFetchTimeAfterOrderByFetchTimeAsc(since);
    }

    public List<GoldPrice> getHistoryBetween(LocalDateTime start, LocalDateTime end) {
        return goldPriceRepository.findByFetchTimeBetweenOrderByFetchTimeAsc(start, end);
    }

    public Optional<GoldPrice> getLatestPrice() {
        return goldPriceRepository.findTopByOrderByFetchTimeDesc();
    }

    public Optional<GoldPrice> getLatestBySource(String source) {
        return goldPriceRepository.findTopBySourceAndFetchTimeAfterOrderByFetchTimeDesc(source, LocalDateTime.now().minusHours(24));
    }

    public List<GoldPrice> getHistoryBySource(String source, LocalDateTime since) {
        if (since == null) since = LocalDateTime.now().minusDays(7);
        return goldPriceRepository.findBySourceAndFetchTimeAfterOrderByFetchTimeAsc(source, since);
    }
}
