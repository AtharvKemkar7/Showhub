package com.eventplatform.search.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record IndexEventRequest(
        @NotNull UUID eventId,
        @NotBlank String title,
        String description,
        String city,
        String categoryName,
        @NotBlank String status,
        UUID organizerId,
        Instant startAt
) {
}
