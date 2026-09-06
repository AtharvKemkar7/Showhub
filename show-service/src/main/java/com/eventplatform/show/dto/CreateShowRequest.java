package com.eventplatform.show.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateShowRequest(
        @NotNull UUID eventId,
        @NotNull UUID venueId,
        @NotNull UUID hallId,
        @Size(max = 50) String language,
        @NotNull @Future Instant startAt,
        @NotNull Instant endAt,
        @NotEmpty @Valid List<SeatPriceRequest> prices
) {
}
