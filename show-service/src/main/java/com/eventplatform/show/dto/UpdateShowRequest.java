package com.eventplatform.show.dto;

import com.eventplatform.show.domain.ShowStatus;
import jakarta.validation.Valid;

import java.time.Instant;
import java.util.List;

public record UpdateShowRequest(
        Instant startAt,
        Instant endAt,
        String language,
        ShowStatus status,
        @Valid List<SeatPriceRequest> prices
) {
}
