package com.finflow.notification.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Dev-friendly delivery. Swap for JavaMailSender / SES / Twilio in prod.
 */
@Component
public class MailChannel {

    private static final Logger log = LoggerFactory.getLogger(MailChannel.class);

    private final boolean enabled;

    public MailChannel(@Value("${mail.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public void send(Long customerUserId, String subject, String body) {
        // No email on the event payload yet — address by userId for the learning path.
        if (enabled) {
            log.info("MAIL enabled stub → userId={} subject={} body={}", customerUserId, subject, body);
        } else {
            log.info("MAIL (dev/log only) → userId={} subject={} body={}", customerUserId, subject, body);
        }
    }
}
