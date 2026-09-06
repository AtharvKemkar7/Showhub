package com.eventplatform.booking.dto;

import com.eventplatform.booking.domain.Booking;
import com.eventplatform.booking.domain.BookingStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID customerId,
        UUID showId,
        UUID eventId,
        UUID organizerId,
        UUID venueId,
        UUID hallId,
        UUID lockId,
        Instant lockExpiresAt,
        BookingStatus status,
        int totalCents,
        String currency,
        UUID paymentId,
        String cancelReason,
        List<Item> items,
        Instant createdAt,
        Instant updatedAt,
        Instant confirmedAt,
        Instant cancelledAt
) {
    public record Item(
            UUID seatId,
            String rowLabel,
            int seatNumber,
            String seatType,
            int amountCents,
            String currency
    ) {
    }

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getCustomerId(),
                booking.getShowId(),
                booking.getEventId(),
                booking.getOrganizerId(),
                booking.getVenueId(),
                booking.getHallId(),
                booking.getLockId(),
                booking.getLockExpiresAt(),
                booking.getStatus(),
                booking.getTotalCents(),
                booking.getCurrency(),
                booking.getPaymentId(),
                booking.getCancelReason(),
                booking.getItems().stream()
                        .map(i -> new Item(i.getSeatId(), i.getRowLabel(), i.getSeatNumber(), i.getSeatType(), i.getAmountCents(), i.getCurrency()))
                        .toList(),
                booking.getCreatedAt(),
                booking.getUpdatedAt(),
                booking.getConfirmedAt(),
                booking.getCancelledAt()
        );
    }
}
