package com.goldmonitor.scheduler;

import com.goldmonitor.model.GoldPrice;
import com.goldmonitor.service.AlertService;
import com.goldmonitor.service.CrawlerService;
import com.goldmonitor.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GoldPriceScheduler {

    private static final Logger log = LoggerFactory.getLogger(GoldPriceScheduler.class);

    private final CrawlerService crawlerService;
    private final AlertService alertService;
    private final NotificationService notificationService;

    @Value("${crawler.enabled}")
    private boolean crawlerEnabled;

    private volatile boolean manualOverride = false;
    private volatile Boolean manualState = null;

    public boolean isCrawlerRunning() {
        return manualState != null ? manualState : crawlerEnabled;
    }

    public void setCrawlerRunning(boolean running) {
        this.manualState = running;
    }

    public GoldPriceScheduler(CrawlerService crawlerService,
                              AlertService alertService,
                              NotificationService notificationService) {
        this.crawlerService = crawlerService;
        this.alertService = alertService;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedRateString = "#{${crawler.interval-minutes} * 60 * 1000}")
    public void fetchAndCheck() {
        if (!isCrawlerRunning()) return;

        log.info("开始抓取金价...");
        List<GoldPrice> prices = crawlerService.fetchPrices();

        String preferred = notificationService.getPreferredSource();
        GoldPrice target = prices.stream()
                .filter(p -> p.getSource().contains(preferred != null ? preferred : "上海黄金交易所"))
                .findFirst().orElse(null);

        if (target == null && !prices.isEmpty()) target = prices.get(0);
        if (target != null) {
            List<String> triggered = alertService.evaluateRules(target);
            if (!triggered.isEmpty()) {
                notificationService.sendAlert("金价监控通知", String.join("\n\n", triggered), target.getPrice());
            }
        }
    }

    @Scheduled(cron = "0 0 20 * * ?")
    public void dailyReport() {
        notificationService.sendDailyReport();
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanup() {
        notificationService.cleanupOldData();
    }
}
