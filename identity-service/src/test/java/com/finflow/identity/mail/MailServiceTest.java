package com.finflow.identity.mail;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MailServiceTest {

    @Test
    void disabledModeSkipsSmtpAndDoesNotLogTokenBody() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        Logger logger = (Logger) LoggerFactory.getLogger(MailService.class);
        Level originalLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
        try {
            new MailService(false, "", provider)
                    .send("member@finflow.test", "Verification", "Secret token: fixture-secret-token");

            verifyNoInteractions(provider);
            assertTrue(appender.list.stream().anyMatch(event ->
                    event.getFormattedMessage().contains("delivery=skipped")));
            assertFalse(appender.list.stream().anyMatch(event ->
                    event.getFormattedMessage().contains("fixture-secret-token")));
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(originalLevel);
            appender.stop();
        }
    }

    @Test
    void enabledModeSendsViaSmtpWithoutLoggingTokenBody() {
        JavaMailSender sender = mock(JavaMailSender.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);

        Logger logger = (Logger) LoggerFactory.getLogger(MailService.class);
        Level originalLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
        try {
            new MailService(true, "noreply@finflow.test", provider)
                    .send("member@finflow.test", "Verification", "Secret token: fixture-secret-token");

            verify(sender).send(any(SimpleMailMessage.class));
            assertTrue(appender.list.stream().anyMatch(event ->
                    event.getFormattedMessage().contains("MAIL sent")));
            assertFalse(appender.list.stream().anyMatch(event ->
                    event.getFormattedMessage().contains("fixture-secret-token")));
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(originalLevel);
            appender.stop();
        }
    }

    @Test
    void enabledModeWithoutSenderFailsClearly() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        assertThrows(IllegalStateException.class, () ->
                new MailService(true, "noreply@finflow.test", provider)
                        .send("member@finflow.test", "Verification", "token"));
    }
}
