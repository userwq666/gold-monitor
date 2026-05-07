package com.goldmonitor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sender_mails")
public class SenderMail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String host = "smtp.qq.com";

    @Column(nullable = false)
    private int port = 587;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private boolean enabled = true;
}
