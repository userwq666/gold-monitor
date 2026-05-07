package com.goldmonitor.dto;

import lombok.Data;

@Data
public class RecipientRequest {
    private String email;
    private String name;
    private Boolean enabled;
}
