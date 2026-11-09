package com.finflow.commerce.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Logging implementation of CommerceEventPublisher.
 * This publisher does not send messages to Kafka. It writes the event's important fields to application logs.
 * Spring activates this implementation when `commerce.events.kafka-enabled=false`. It is also activated when the property is missing. This makes logging the default behaviour for local development.
 */
@Component
@ConditionalOnProperty(
        name = "commerce.events.kafka-enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class LoggingCommerceEventPublisher implements CommerceEventPublisher {

    /**
     * Logger associated with this class.
     * SLF4J provides a common logging API. The actual logging implementation is selected by the application.
     * static: one logger reference is shared by all instances. final: the reference cannot be reassigned.
     */
    private static final Logger log = LoggerFactory.getLogger(LoggingCommerceEventPublisher.class); // Logger associated with this class.

    /**
     * Publishes an event by writing selected event fields to the log.
     * This is not durable message delivery. The event is not stored in Kafka or another message broker.
     * @param event the Commerce domain event to publish
     */
    @Override
    public void publish(CommerceDomainEvent event) {
        log.info(
                "commerce.event type={} orderId={} orgId={} amount={}",
                event.type(),
                event.orderId(),
                event.organisationId(),
                event.totalAmount()
        );
    }
}