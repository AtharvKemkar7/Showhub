package com.eventplatform.venue.dto;

import com.eventplatform.venue.domain.Venue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VenueResponse(
        UUID id,
        String name,
        String address,
        String city,
        String state,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        List<HallResponse> halls
) {
    public static VenueResponse from(Venue venue, List<HallResponse> halls) {
        return new VenueResponse(
                venue.getId(),
                venue.getName(),
                venue.getAddress(),
                venue.getCity(),
                venue.getState(),
                venue.isActive(),
                venue.getCreatedAt(),
                venue.getUpdatedAt(),
                halls
        );
    }

    public static VenueResponse summary(Venue venue) {
        return from(venue, List.of());
    }
}
