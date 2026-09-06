package com.eventplatform.event.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateEventRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 8000) String description,
        @NotBlank @Size(max = 200) String venueName,
        @NotBlank @Size(max = 300) String venueAddress,
        @NotBlank @Size(max = 100) String city,
        @NotNull @Future Instant startAt,
        @NotNull Instant endAt
) {
}
