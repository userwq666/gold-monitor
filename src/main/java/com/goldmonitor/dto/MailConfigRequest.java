package com.goldmonitor.dto;

import lombok.Data;

@Data
public class MailConfigRequest {
    private String host;
    private Integer port;
    private String username;
    private String password;
    private Boolean enabled;
}
