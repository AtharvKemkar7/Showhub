package com.eventplatform.search.dto;

import com.eventplatform.search.domain.SearchEvent;

import java.time.Instant;
import java.util.UUID;

public record SearchEventResponse(
        UUID eventId,
        String title,
        String description,
        String city,
        String categoryName,
        String status,
        UUID organizerId,
        Instant startAt,
        Instant indexedAt
) {
    public static SearchEventResponse from(SearchEvent event) {
        return new SearchEventResponse(
                event.getEventId(),
                event.getTitle(),
                event.getDescription(),
                event.getCity(),
                event.getCategoryName(),
                event.getStatus(),
                event.getOrganizerId(),
                event.getStartAt(),
                event.getIndexedAt()
        );
    }
}
