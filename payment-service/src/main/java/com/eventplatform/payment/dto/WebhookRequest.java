package com.eventplatform.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record WebhookRequest(
        @NotNull UUID paymentId,
        @NotBlank String status,
        String providerRef,
        String failureReason
) {
}
