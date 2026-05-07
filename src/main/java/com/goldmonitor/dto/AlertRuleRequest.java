package com.goldmonitor.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AlertRuleRequest {
    private String name;
    private String type;
    private BigDecimal threshold;
    private String direction;
    private String time;
    private Boolean enabled;
}
