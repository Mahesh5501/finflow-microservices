package com.finflow.identity.mail;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Fail fast when mail is on but SMTP is incomplete — avoids "register OK, no email".
 */
@Component
@ConditionalOnProperty(name = "mail.enabled", havingValue = "true")
class MailDeliveryGuard {

    private final String from;
    private final ObjectProvider<JavaMailSender> mailSender;

    MailDeliveryGuard(
            @Value("${mail.from:}") String from,
            ObjectProvider<JavaMailSender> mailSender
    ) {
        this.from = from == null ? "" : from.trim();
        this.mailSender = mailSender;
    }

    @PostConstruct
    void requireSmtp() {
        if (from.isBlank()) {
            throw new IllegalStateException(
                    "mail.enabled=true needs mail.from (set it in application-local.properties)"
            );
        }
        if (mailSender.getIfAvailable() == null) {
            throw new IllegalStateException(
                    "mail.enabled=true needs spring.mail.host/username/password "
                            + "(copy application-local.properties.example)"
            );
        }
    }
}
