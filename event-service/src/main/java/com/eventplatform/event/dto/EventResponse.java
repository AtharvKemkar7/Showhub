package com.eventplatform.event.dto;

import com.eventplatform.event.domain.Event;
import com.eventplatform.event.domain.EventStatus;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
        UUID id,
        UUID organizerId,
        EventCategoryResponse category,
        String title,
        String description,
        String venueName,
        String venueAddress,
        String city,
        Instant startAt,
        Instant endAt,
        EventStatus status,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getOrganizerId(),
                EventCategoryResponse.from(event.getCategory()),
                event.getTitle(),
                event.getDescription(),
                event.getVenueName(),
                event.getVenueAddress(),
                event.getCity(),
                event.getStartAt(),
                event.getEndAt(),
                event.getStatus(),
                event.getRejectionReason(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
