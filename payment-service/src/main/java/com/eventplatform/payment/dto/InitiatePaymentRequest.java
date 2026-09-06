package com.eventplatform.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InitiatePaymentRequest(
        @NotNull UUID bookingId,
        @NotNull @Min(0) Integer amountCents,
        String currency
) {
}
