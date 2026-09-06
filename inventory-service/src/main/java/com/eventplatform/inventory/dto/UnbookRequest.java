package com.eventplatform.inventory.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UnbookRequest(
        @NotNull UUID bookingId
) {
}
