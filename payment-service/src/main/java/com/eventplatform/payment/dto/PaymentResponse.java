package com.eventplatform.payment.dto;

import com.eventplatform.payment.domain.Payment;
import com.eventplatform.payment.domain.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID bookingId,
        UUID customerId,
        int amountCents,
        String currency,
        PaymentStatus status,
        String provider,
        String providerRef,
        String checkoutUrl,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBookingId(),
                payment.getCustomerId(),
                payment.getAmountCents(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getProvider(),
                payment.getProviderRef(),
                "/api/v1/payments/" + payment.getId() + "/mock-checkout",
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
