package com.eventplatform.event.dto;

import com.eventplatform.event.domain.EventCategory;

import java.time.Instant;
import java.util.UUID;

public record EventCategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
    public static EventCategoryResponse from(EventCategory category) {
        return new EventCategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
