package com.finflow.identity.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends mail when {@code mail.enabled=true}. When off, skips send so APIs can return tokens for Postman.
 * Never log message bodies — they contain one-time secrets.
 */
@Slf4j
@Service
public class MailService {

    private final boolean enabled;
    private final String from;
    private final ObjectProvider<JavaMailSender> mailSender;

    public MailService(
            @Value("${mail.enabled:false}") boolean enabled,
            @Value("${mail.from:}") String from,
            ObjectProvider<JavaMailSender> mailSender
    ) {
        this.enabled = enabled;
        this.from = from == null ? "" : from.trim();
        this.mailSender = mailSender;
    }

    public void send(String to, String subject, String body) {
        if (!enabled) {
            log.info("MAIL skipped (mail.enabled=false) to={} subject={}", to, subject);
            return;
        }

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException(
                    "mail.enabled=true but spring.mail.* is missing — use application-local.properties"
            );
        }
        if (from.isBlank()) {
            throw new IllegalStateException("mail.enabled=true requires mail.from");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
        log.info("MAIL sent to={} subject={}", to, subject);
    }

    /** When true, APIs must not return raw tokens — the user receives them by email. */
    public boolean isEnabled() {
        return enabled;
    }
}
