package com.finflow.transaction.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "transaction.events.kafka-enabled", havingValue = "true")
public class KafkaTransactionEventPublisher implements TransactionEventPublisher {

    private final KafkaTemplate<String, TransactionDomainEvent> kafkaTemplate;
    private final String topic;

    public KafkaTransactionEventPublisher(
            KafkaTemplate<String, TransactionDomainEvent> kafkaTemplate,
            @Value("${transaction.events.topic:transaction.events}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(TransactionDomainEvent event) {
        kafkaTemplate.send(topic, String.valueOf(event.transactionId()), event);
    }
}
