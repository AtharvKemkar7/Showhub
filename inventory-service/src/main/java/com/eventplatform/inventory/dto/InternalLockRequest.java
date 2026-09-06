package com.eventplatform.inventory.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record InternalLockRequest(
        @NotNull UUID ownerId,
        @NotNull UUID showId,
        @NotEmpty List<UUID> seatIds
) {
}
