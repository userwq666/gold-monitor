package com.goldmonitor.controller;

import com.goldmonitor.scheduler.GoldPriceScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HealthController {

    private final GoldPriceScheduler scheduler;

    public HealthController(GoldPriceScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @GetMapping("/api/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "time", LocalDateTime.now().toString(),
                "crawler", scheduler.isCrawlerRunning()
        ));
    }

    @PostMapping("/api/crawler/toggle")
    public ResponseEntity<?> toggleCrawler(@RequestBody Map<String, Boolean> body) {
        boolean running = body.getOrDefault("running", false);
        scheduler.setCrawlerRunning(running);
        return ResponseEntity.ok(Map.of("running", running));
    }

    @GetMapping("/api/crawler/status")
    public ResponseEntity<?> crawlerStatus() {
        return ResponseEntity.ok(Map.of("running", scheduler.isCrawlerRunning()));
    }
}
