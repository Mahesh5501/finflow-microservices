package com.finflow.identity.mail;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailServiceTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void stubKeepsDiagnosticsWithoutLoggingTokenBearingBody(boolean enabled) {
        Logger logger = (Logger) LoggerFactory.getLogger(MailService.class);
        Level originalLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
        try {
            new MailService(enabled).send("member@finflow.test", "Verification", "Secret token: fixture-secret-token");

            assertTrue(appender.list.stream().anyMatch(event ->
                    event.getFormattedMessage().contains("delivery=not-implemented")));
            assertFalse(appender.list.stream().anyMatch(event ->
                    event.getFormattedMessage().contains("fixture-secret-token")));
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(originalLevel);
            appender.stop();
        }
    }
}
