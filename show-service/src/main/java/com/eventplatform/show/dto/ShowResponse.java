package com.eventplatform.show.dto;

import com.eventplatform.show.domain.SeatType;
import com.eventplatform.show.domain.Show;
import com.eventplatform.show.domain.ShowStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShowResponse(
        UUID id,
        UUID eventId,
        UUID organizerId,
        UUID venueId,
        UUID hallId,
        String language,
        Instant startAt,
        Instant endAt,
        ShowStatus status,
        List<Price> prices,
        Instant createdAt,
        Instant updatedAt
) {
    public record Price(SeatType seatType, int amountCents, String currency) {
    }

    public static ShowResponse from(Show show) {
        return new ShowResponse(
                show.getId(),
                show.getEventId(),
                show.getOrganizerId(),
                show.getVenueId(),
                show.getHallId(),
                show.getLanguage(),
                show.getStartAt(),
                show.getEndAt(),
                show.getStatus(),
                show.getPrices().stream()
                        .map(p -> new Price(p.getSeatType(), p.getAmountCents(), p.getCurrency()))
                        .toList(),
                show.getCreatedAt(),
                show.getUpdatedAt()
        );
    }
}
