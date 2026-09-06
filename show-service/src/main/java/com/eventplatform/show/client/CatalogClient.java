package com.eventplatform.show.client;

import java.util.UUID;

public interface CatalogClient {

    EventInfo requirePublishedOrOwnedEvent(UUID eventId, UUID requesterId, boolean admin);

    VenueInfo requireVenueHall(UUID venueId, UUID hallId);

    record EventInfo(UUID id, UUID organizerId, String status, String title, boolean published) {
    }

    record VenueInfo(UUID venueId, UUID hallId, String venueName, String hallName, boolean active) {
    }
}
