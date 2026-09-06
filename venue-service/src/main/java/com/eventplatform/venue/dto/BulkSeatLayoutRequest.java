package com.eventplatform.venue.dto;

import com.eventplatform.venue.domain.SeatType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkSeatLayoutRequest(
        @NotEmpty List<@Size(max = 10) String> rows,
        @NotNull @Min(1) Integer seatsPerRow,
        @NotNull SeatType defaultType
) {
}
