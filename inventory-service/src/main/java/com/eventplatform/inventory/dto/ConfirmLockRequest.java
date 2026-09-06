package com.eventplatform.inventory.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConfirmLockRequest(
        @NotNull UUID lockId,
        @NotNull UUID bookingId
) {
}
