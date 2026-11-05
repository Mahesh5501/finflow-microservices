package com.finflow.transaction.event;

public interface TransactionEventPublisher {

    void publish(TransactionDomainEvent event);
}
