package com.eventplatform.inventory.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LockResponse(
        UUID lockId,
        UUID showId,
        UUID ownerId,
        Instant expiresAt,
        List<UUID> seatIds,
        String status
) {
}
