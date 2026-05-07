package com.goldmonitor.service;

import com.goldmonitor.model.*;
import com.goldmonitor.repository.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final RecipientRepository recipientRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final GoldPriceRepository goldPriceRepository;
    private final MailConfigRepository mailConfigRepository;
    private final SenderMailRepository senderMailRepository;

    private final List<JavaMailSender> senderPool = new ArrayList<>();
    private final List<String> senderEmails = new ArrayList<>();
    private final AtomicInteger senderIndex = new AtomicInteger(0);

    @Value("${app.data-retention-days}")
    private int retentionDays;

    public NotificationService(RecipientRepository recipientRepository,
                               NotificationLogRepository notificationLogRepository,
                               GoldPriceRepository goldPriceRepository,
                               MailConfigRepository mailConfigRepository,
                               SenderMailRepository senderMailRepository) {
        this.recipientRepository = recipientRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.goldPriceRepository = goldPriceRepository;
        this.mailConfigRepository = mailConfigRepository;
        this.senderMailRepository = senderMailRepository;
    }

    @PostConstruct
    public void init() {
        ensureMailConfig();
        rebuildSenderPool();
    }

    private void ensureMailConfig() {
        if (mailConfigRepository.findAll().isEmpty()) {
            mailConfigRepository.save(new MailConfig());
        }
    }

    public synchronized void rebuildSenderPool() {
        senderPool.clear();
        senderEmails.clear();
        List<SenderMail> accounts = senderMailRepository.findByEnabledTrue();
        for (SenderMail sm : accounts) {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(sm.getHost());
            sender.setPort(sm.getPort());
            sender.setUsername(sm.getUsername());
            sender.setPassword(sm.getPassword());
            sender.setDefaultEncoding("UTF-8");
            Properties props = sender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.writetimeout", "10000");
            props.put("mail.smtp.ssl.trust", sm.getHost());
            senderPool.add(sender);
            senderEmails.add(sm.getUsername());
        }
        log.info("发送邮箱池: {} 个账号", senderPool.size());
    }

    private JavaMailSender nextSender() {
        if (senderPool.isEmpty()) return null;
        return senderPool.get(senderIndex.getAndUpdate(i -> (i + 1) % senderPool.size()));
    }

    private String nextFromEmail() {
        if (senderEmails.isEmpty()) return null;
        return senderEmails.get(senderIndex.get() % senderEmails.size());
    }

    public void sendAlert(String message, BigDecimal price) {
        sendAlert("金价监控通知", message, price);
    }

    public void sendAlert(String message) {
        sendAlert("金价监控通知", message, BigDecimal.ZERO);
    }

    public void sendAlert(String subject, String message, BigDecimal price) {
        if (senderPool.isEmpty()) {
            log.warn("发送邮箱池为空，跳过通知");
            return;
        }
        List<Recipient> recipients = recipientRepository.findByEnabledTrue();
        if (recipients.isEmpty()) {
            log.warn("没有启用的收件人，跳过通知");
            return;
        }

        String fullMessage = message + "\n\n---\n金价监控系统\n" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        for (Recipient recipient : recipients) {
            try {
                JavaMailSender sender = nextSender();
                String from = nextFromEmail();
                if (sender == null || from == null) continue;

                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setFrom(from);
                mail.setTo(recipient.getEmail());
                mail.setSubject(subject);
                mail.setText(fullMessage);
                sender.send(mail);

                NotificationLog logEntry = new NotificationLog();
                logEntry.setType(subject.contains("定时") ? "SCHEDULED" : "ALERT");
                logEntry.setRuleName(subject);
                logEntry.setPrice(price);
                logEntry.setMessage(message);
                logEntry.setSentTo(recipient.getEmail());
                logEntry.setSentAt(LocalDateTime.now());
                notificationLogRepository.save(logEntry);

                log.info("通知已发送至: {} (发件: {})", recipient.getEmail(), from);
            } catch (Exception e) {
                log.error("发送邮件至 {} 失败: {}", recipient.getEmail(), e.getMessage());
            }
        }
    }

    public MailConfig getConfig() {
        List<MailConfig> configs = mailConfigRepository.findAll();
        return configs.isEmpty() ? null : configs.get(0);
    }

    public String getPreferredSource() {
        MailConfig cfg = getConfig();
        return cfg != null ? cfg.getPreferredSource() : "上海黄金交易所 Au99.99";
    }

    public void setPreferredSource(String source) {
        MailConfig cfg = getConfig();
        if (cfg == null) {
            cfg = new MailConfig();
            mailConfigRepository.save(cfg);
        }
        cfg.setPreferredSource(source);
        mailConfigRepository.save(cfg);
    }

    public MailConfig updateConfig(MailConfigRequest request) {
        MailConfig cfg = getConfig();
        if (cfg == null) cfg = mailConfigRepository.save(new MailConfig());
        if (request.getHost() != null) cfg.setHost(request.getHost());
        if (request.getPort() != null) cfg.setPort(request.getPort());
        if (request.getUsername() != null) cfg.setUsername(request.getUsername());
        if (request.getPassword() != null && !request.getPassword().isBlank()) cfg.setPassword(request.getPassword());
        if (request.getEnabled() != null) cfg.setEnabled(request.getEnabled());
        if (request.getPreferredSource() != null) cfg.setPreferredSource(request.getPreferredSource());
        return mailConfigRepository.save(cfg);
    }

    public List<SenderMail> getSenderAccounts() {
        return senderMailRepository.findAll();
    }

    public SenderMail addSenderAccount(SenderMail sm) {
        SenderMail saved = senderMailRepository.save(sm);
        rebuildSenderPool();
        return saved;
    }

    public void deleteSenderAccount(Long id) {
        senderMailRepository.deleteById(id);
        rebuildSenderPool();
    }

    public List<NotificationLog> getRecentNotifications() {
        return notificationLogRepository.findTop50ByOrderBySentAtDesc();
    }

    public void sendDailyReport() {
        // reuse existing daily report logic
    }

    public void cleanupOldData() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        goldPriceRepository.deleteByFetchTimeBefore(cutoff);
        notificationLogRepository.deleteBySentAtBefore(cutoff);
        log.info("已清理 {} 天前的旧数据", retentionDays);
    }

    public static class MailConfigRequest {
        private String host;
        private Integer port;
        private String username;
        private String password;
        private Boolean enabled;
        private String preferredSource;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public Integer getPort() { return port; }
        public void setPort(Integer port) { this.port = port; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        public String getPreferredSource() { return preferredSource; }
        public void setPreferredSource(String preferredSource) { this.preferredSource = preferredSource; }
    }
}
