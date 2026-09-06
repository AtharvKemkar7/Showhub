package com.eventplatform.common.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);
    private final ObjectMapper objectMapper;

    public LoggingEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(String topic, String key, DomainEvent event) {
        try {
            log.info("domain-event topic={} key={} type={} aggregateId={}", topic, key, event.type(), event.aggregateId());
            objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize domain event type={}", event.type());
        }
    }
}
