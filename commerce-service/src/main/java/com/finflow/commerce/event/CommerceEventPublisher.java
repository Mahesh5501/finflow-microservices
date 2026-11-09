package com.finflow.commerce.event;

/**
 * Abstraction for publishing Commerce Service events.
 *
 * Business services depend on this interface rather than depending
 * directly on Kafka or a logging implementation.
 *
 * Current implementations:
 * 1. LoggingCommerceEventPublisher
 * 2. KafkaCommerceEventPublisher
 */
public interface CommerceEventPublisher {

    /**
     * Publishes a CommerceDomainEvent.
     *
     * The implementation decides how and where the event is delivered.
     *
     * Current implementations:
     * - Logging publisher writes an event to application logs.
     * - Kafka publisher sends an event to a Kafka topic.
     *
     * The current contract returns void, so callers do not receive
     * a delivery acknowledgement or asynchronous result.
     *
     * @param event event data to publish
     */
    void publish(CommerceDomainEvent event);
}