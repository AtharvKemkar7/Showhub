package com.eventplatform.common.messaging;

public interface EventPublisher {
    void publish(String topic, String key, DomainEvent event);
}
