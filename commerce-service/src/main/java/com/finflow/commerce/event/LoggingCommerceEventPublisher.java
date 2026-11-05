package com.finflow.commerce.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Default when Kafka is off — still proves the publish hook in tests/local. */
@Component
@ConditionalOnProperty(name = "commerce.events.kafka-enabled", havingValue = "false", matchIfMissing = true)
public class LoggingCommerceEventPublisher implements CommerceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingCommerceEventPublisher.class);

    @Override
    public void publish(CommerceDomainEvent event) {
        log.info("commerce.event type={} orderId={} orgId={} amount={}",
                event.type(), event.orderId(), event.organisationId(), event.totalAmount());
    }
}
