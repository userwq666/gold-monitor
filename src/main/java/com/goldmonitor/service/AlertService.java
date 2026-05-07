package com.goldmonitor.service;

import com.goldmonitor.model.AlertRule;
import com.goldmonitor.model.GoldPrice;
import com.goldmonitor.repository.AlertRuleRepository;
import com.goldmonitor.repository.GoldPriceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRuleRepository alertRuleRepository;
    private final GoldPriceRepository goldPriceRepository;

    public AlertService(AlertRuleRepository alertRuleRepository,
                        GoldPriceRepository goldPriceRepository) {
        this.alertRuleRepository = alertRuleRepository;
        this.goldPriceRepository = goldPriceRepository;
    }

    public List<String> evaluateRules(GoldPrice currentPrice) {
        List<String> triggered = new ArrayList<>();
        List<AlertRule> rules = alertRuleRepository.findByEnabledTrue();

        for (AlertRule rule : rules) {
            String result = evaluate(rule, currentPrice);
            if (result != null) {
                rule.setLastTriggeredAt(LocalDateTime.now());
                rule.setLastTriggeredPrice(currentPrice.getPrice());
                alertRuleRepository.save(rule);
                triggered.add(result);
            }
        }
        return triggered;
    }

    private String evaluate(AlertRule rule, GoldPrice current) {
        String result = switch (rule.getType()) {
            case "THRESHOLD" -> evaluateThreshold(rule, current);
            case "CHANGE" -> evaluateChange(rule, current);
            case "EXTREME" -> evaluateExtreme(rule, current);
            case "SCHEDULED" -> evaluateScheduled(rule, current);
            default -> null;
        };
        return result;
    }

    private String evaluateThreshold(AlertRule rule, GoldPrice current) {
        if (rule.getThreshold() == null) return null;
        BigDecimal threshold = rule.getThreshold();
        BigDecimal currentPrice = current.getPrice();
        String source = current.getSource();

        List<GoldPrice> recent = goldPriceRepository.findBySourceAndFetchTimeAfterOrderByFetchTimeAsc(
                current.getSource(), LocalDateTime.now().minusMinutes(10));
        if (recent.size() < 2) return null;
        BigDecimal prevPrice = recent.get(recent.size() - 2).getPrice();

        if ("ABOVE".equals(rule.getDirection())) {
            if (prevPrice.compareTo(threshold) < 0 && currentPrice.compareTo(threshold) >= 0) {
                return String.format("[阈值触发] %s: 金价突破 %.2f 元/克，当前 %.2f 元/克 (%s)",
                        rule.getName(), threshold, currentPrice, source);
            }
        }
        if ("BELOW".equals(rule.getDirection())) {
            if (prevPrice.compareTo(threshold) > 0 && currentPrice.compareTo(threshold) <= 0) {
                return String.format("[阈值触发] %s: 金价跌破 %.2f 元/克，当前 %.2f 元/克 (%s)",
                        rule.getName(), threshold, currentPrice, source);
            }
        }
        return null;
    }

    private String evaluateChange(AlertRule rule, GoldPrice current) {
        if (rule.getThreshold() == null) return null;
        String source = current.getSource();
        BigDecimal base;
        if (rule.getLastTriggeredPrice() != null) {
            base = rule.getLastTriggeredPrice();
        } else {
            List<GoldPrice> recent = goldPriceRepository.findBySourceAndFetchTimeAfterOrderByFetchTimeAsc(
                    current.getSource(), LocalDateTime.now().minusMinutes(10));
            if (recent.size() < 2) return null;
            base = recent.get(recent.size() - 2).getPrice();
        }
        BigDecimal diff = current.getPrice().subtract(base);
        if (diff.abs().compareTo(rule.getThreshold()) >= 0) {
            String direction = diff.compareTo(BigDecimal.ZERO) >= 0 ? "上涨" : "下跌";
            return String.format("[涨跌触发] %s: 金价%s %.2f 元/克 (变化 %+.2f 元/克，阈值 %.2f 元/克) (%s)",
                    rule.getName(), direction, current.getPrice(), diff, rule.getThreshold(), source);
        }
        return null;
    }

    private String evaluateExtreme(AlertRule rule, GoldPrice current) {
        if (rule.getLastTriggeredAt() != null &&
                rule.getLastTriggeredAt().toLocalDate().equals(LocalDate.now())) {
            return null;
        }
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        LocalDateTime before = LocalDateTime.now().minusMinutes(10);
        String source = current.getSource();
        BigDecimal max = goldPriceRepository.findMaxPriceForSourceBetween(
                current.getSource(), since, before);
        BigDecimal min = goldPriceRepository.findMinPriceForSourceBetween(
                current.getSource(), since, before);

        List<String> msgs = new ArrayList<>();
        if (max != null && current.getPrice().compareTo(max) > 0) {
            msgs.add(String.format("[极值触发] %s: 当前金价 %.2f 元/克，创7日新高 (%s)", rule.getName(), current.getPrice(), source));
        }
        if (min != null && current.getPrice().compareTo(min) < 0) {
            msgs.add(String.format("[极值触发] %s: 当前金价 %.2f 元/克，创7日新低 (%s)", rule.getName(), current.getPrice(), source));
        }
        return msgs.isEmpty() ? null : String.join("\n", msgs);
    }

    private String evaluateScheduled(AlertRule rule, GoldPrice current) {
        if (rule.getTime() == null) return null;
        if (rule.getLastTriggeredAt() != null &&
                rule.getLastTriggeredAt().toLocalDate().equals(LocalDate.now())) {
            return null;
        }
        LocalTime now = LocalTime.now();
        LocalTime target = LocalTime.parse(rule.getTime());
        if (now.isAfter(target) || now.equals(target)) {
            return String.format("[定时报告] %s: 当前金价 %.2f 元/克 (%s)", rule.getName(), current.getPrice(), current.getSource());
        }
        return null;
    }

    public AlertRule createRule(AlertRule rule) {
        return alertRuleRepository.save(rule);
    }

    public AlertRule updateRule(Long id, AlertRule rule) {
        rule.setId(id);
        return alertRuleRepository.save(rule);
    }

    public void deleteRule(Long id) {
        alertRuleRepository.deleteById(id);
    }

    public List<AlertRule> getAllRules() {
        return alertRuleRepository.findAll();
    }
}
