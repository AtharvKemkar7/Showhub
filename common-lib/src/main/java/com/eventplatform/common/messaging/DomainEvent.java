package com.eventplatform.common.messaging;

import java.time.Instant;
import java.util.UUID;

public record DomainEvent(
        String type,
        String aggregateId,
        UUID actorId,
        Instant occurredAt,
        String payloadJson
) {
    public static DomainEvent of(String type, String aggregateId, UUID actorId, String payloadJson) {
        return new DomainEvent(type, aggregateId, actorId, Instant.now(), payloadJson);
    }
}
