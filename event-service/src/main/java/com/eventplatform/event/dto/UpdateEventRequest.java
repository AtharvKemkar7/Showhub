package com.eventplatform.event.dto;

import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record UpdateEventRequest(
        UUID categoryId,
        @Size(max = 200) String title,
        @Size(max = 8000) String description,
        @Size(max = 200) String venueName,
        @Size(max = 300) String venueAddress,
        @Size(max = 100) String city,
        Instant startAt,
        Instant endAt
) {
}
