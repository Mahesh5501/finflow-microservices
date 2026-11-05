package com.finflow.identity.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Local-friendly mail.
 * When {@code mail.enabled=false} (default), logs the link and APIs also return the raw token for Postman.
 * Enable + configure SMTP for real send in prod.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final boolean enabled;

    public MailService(@Value("${mail.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public void send(String to, String subject, String body) {
        if (enabled) {
            // Hook point for JavaMailSender in prod — intentionally logged locally for FinFlow learning.
            log.info("MAIL enabled stub → to={} subject={} body={}", to, subject, body);
        } else {
            log.info("MAIL (dev/log only) → to={} subject={} body={}", to, subject, body);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
