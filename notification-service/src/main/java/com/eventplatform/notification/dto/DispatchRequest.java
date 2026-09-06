package com.eventplatform.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DispatchRequest(
        @NotNull UUID userId,
        @NotBlank String eventType,
        String aggregateId,
        String payloadJson
) {
}
