package com.goldmonitor.controller;

import com.goldmonitor.model.MailConfig;
import com.goldmonitor.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mail-config")
public class MailConfigController {

    private final NotificationService notificationService;

    public MailConfigController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<?> getConfig() {
        MailConfig cfg = notificationService.getConfig();
        if (cfg == null) {
            return ResponseEntity.ok(Map.of());
        }
        String pw = cfg.getPassword();
        cfg.setPassword(pw != null && !pw.isBlank() ? "******" : "");
        return ResponseEntity.ok(cfg);
    }

    @PutMapping
    public ResponseEntity<?> updateConfig(@RequestBody NotificationService.MailConfigRequest request) {
        MailConfig cfg = notificationService.updateConfig(request);
        cfg.setPassword(cfg.getPassword() != null && !cfg.getPassword().isBlank() ? "******" : "");
        return ResponseEntity.ok(cfg);
    }

    @PutMapping("/preferred-source")
    public ResponseEntity<?> setPreferredSource(@RequestBody Map<String, String> body) {
        String source = body.get("source");
        notificationService.setPreferredSource(source);
        return ResponseEntity.ok(Map.of("message", "已更新"));
    }
}
