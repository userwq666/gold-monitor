package com.goldmonitor.controller;

import com.goldmonitor.model.GoldPrice;
import com.goldmonitor.service.AlertService;
import com.goldmonitor.service.CrawlerService;
import com.goldmonitor.service.NotificationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/gold-prices")
public class GoldPriceController {

    private final CrawlerService crawlerService;
    private final AlertService alertService;
    private final NotificationService notificationService;

    public GoldPriceController(CrawlerService crawlerService,
                               AlertService alertService,
                               NotificationService notificationService) {
        this.crawlerService = crawlerService;
        this.alertService = alertService;
        this.notificationService = notificationService;
    }

    @GetMapping("/latest")
    public ResponseEntity<?> getLatest(@RequestParam(required = false) String source) {
        Optional<GoldPrice> price;
        if (source != null && !source.isBlank()) {
            price = crawlerService.getLatestBySource(source);
        } else {
            price = crawlerService.getLatestPrice();
        }
        return price.isPresent()
                ? ResponseEntity.ok(price.get())
                : ResponseEntity.ok(Map.of("message", "暂无数据"));
    }

    @GetMapping("/history")
    public List<GoldPrice> getHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(required = false) String source) {
        if (since == null) {
            since = LocalDateTime.now().minusDays(7);
        }
        if (source != null && !source.isBlank()) {
            return crawlerService.getHistoryBySource(source, since);
        }
        return crawlerService.getHistory(since);
    }

    @GetMapping("/quotes")
    public ResponseEntity<?> getAllQuotes() {
        return ResponseEntity.ok(crawlerService.fetchAllQuotes());
    }

    @PostMapping("/fetch")
    public ResponseEntity<?> manualFetch(@RequestBody(required = false) Map<String, String> body) {
        String preferredSource = body != null ? body.get("source") : "上海黄金交易所";
        List<GoldPrice> prices = crawlerService.fetchPrices();
        if (!prices.isEmpty()) {
            GoldPrice target = prices.stream()
                    .filter(p -> preferredSource != null && p.getSource().contains(preferredSource))
                    .findFirst().orElse(prices.get(0));
            notificationService.sendAlert("实时金价", String.format("%s: %.2f 元/克",
                    target.getSource(), target.getPrice()), target.getPrice());
            return ResponseEntity.ok(Map.of(
                    "price", target,
                    "message", "已抓取并发送通知"
            ));
        }
        return ResponseEntity.status(503).body(Map.of("error", "抓取失败"));
    }
}
