package com.goldmonitor.controller;

import com.goldmonitor.model.NotificationLog;
import com.goldmonitor.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationLog> getRecent() {
        return notificationService.getRecentNotifications();
    }

    @PostMapping("/test")
    public ResponseEntity<?> sendTest() {
        notificationService.sendAlert("测试邮件", "这是一封测试邮件，金价监控系统配置正常。", java.math.BigDecimal.ZERO);
        return ResponseEntity.ok(Map.of("message", "测试邮件已发送"));
    }
}
