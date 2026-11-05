package com.finflow.commerce.event;

public interface CommerceEventPublisher {

    void publish(CommerceDomainEvent event);
}
