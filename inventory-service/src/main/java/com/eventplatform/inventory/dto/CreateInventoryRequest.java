package com.eventplatform.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateInventoryRequest(
        @NotNull UUID showId,
        @NotEmpty @Valid List<SeatSpec> seats
) {
    public record SeatSpec(
            @NotNull UUID seatId,
            @NotNull String rowLabel,
            @NotNull Integer seatNumber,
            @NotNull String seatType
    ) {
    }
}
