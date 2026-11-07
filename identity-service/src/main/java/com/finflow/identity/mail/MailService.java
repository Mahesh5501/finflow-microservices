package com.finflow.identity.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Local mail stub: no SMTP/provider delivery is implemented for either flag value.
 * The existing mail.enabled flag also controls whether callers expose local development tokens.
 * Accept message content for the future delivery adapter, but never log token-bearing bodies.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final boolean enabled;

    public MailService(@Value("${mail.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public void send(String to, String subject, String body) {
        String mode = enabled ? "enabled stub" : "dev/log only";
        // Keep the diagnostic without leaking verification/reset/invite tokens from body.
        log.info("MAIL ({}) to={} subject={} delivery=not-implemented", mode, to, subject);
    }

    public boolean isEnabled() {
        return enabled;
    }
}
