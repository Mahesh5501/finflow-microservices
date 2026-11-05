package com.finflow.transaction.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "transaction.events.kafka-enabled", havingValue = "false", matchIfMissing = true)
public class LoggingTransactionEventPublisher implements TransactionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingTransactionEventPublisher.class);

    @Override
    public void publish(TransactionDomainEvent event) {
        log.info("transaction.event type={} status={} txId={} amount={}",
                event.type(), event.status(), event.transactionId(), event.amount());
    }
}
