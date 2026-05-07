package com.goldmonitor.controller;

import com.goldmonitor.dto.AlertRuleRequest;
import com.goldmonitor.model.AlertRule;
import com.goldmonitor.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alert-rules")
public class AlertRuleController {

    private final AlertService alertService;

    public AlertRuleController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<AlertRule> getAll() {
        return alertService.getAllRules();
    }

    @PostMapping
    public AlertRule create(@RequestBody AlertRuleRequest request) {
        AlertRule rule = new AlertRule();
        rule.setName(request.getName());
        rule.setType(request.getType());
        rule.setThreshold(request.getThreshold());
        rule.setDirection(request.getDirection());
        rule.setTime(request.getTime());
        rule.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        return alertService.createRule(rule);
    }

    @PutMapping("/{id}")
    public AlertRule update(@PathVariable Long id, @RequestBody AlertRuleRequest request) {
        AlertRule rule = new AlertRule();
        rule.setName(request.getName());
        rule.setType(request.getType());
        rule.setThreshold(request.getThreshold());
        rule.setDirection(request.getDirection());
        rule.setTime(request.getTime());
        rule.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        return alertService.updateRule(id, rule);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        alertService.deleteRule(id);
        return ResponseEntity.ok().build();
    }
}
