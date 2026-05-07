package com.goldmonitor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mail_config")
public class MailConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String host = "smtp.qq.com";

    @Column(nullable = false)
    private int port = 587;

    @Column(nullable = false)
    private String username = "";

    @Column(nullable = false)
    private String password = "";

    private boolean enabled = false;

    @Column(name = "preferred_source")
    private String preferredSource = "工商银行如意金条";
}
