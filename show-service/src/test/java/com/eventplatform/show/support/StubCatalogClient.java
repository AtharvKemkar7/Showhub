package com.eventplatform.show.support;

import com.eventplatform.common.exception.ApiException;
import com.eventplatform.show.client.CatalogClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@TestConfiguration
public class StubCatalogClient implements CatalogClient {

    public static final UUID PUBLISHED_EVENT = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    public static final UUID OTHER_EVENT = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    public static final UUID ORGANIZER = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    public static final UUID OTHER_ORGANIZER = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    public static final UUID VENUE = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    public static final UUID HALL = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    private final Map<UUID, EventInfo> events = new ConcurrentHashMap<>();

    public StubCatalogClient() {
        events.put(PUBLISHED_EVENT, new EventInfo(PUBLISHED_EVENT, ORGANIZER, "PUBLISHED", "Live Night", true));
        events.put(OTHER_EVENT, new EventInfo(OTHER_EVENT, OTHER_ORGANIZER, "PUBLISHED", "Other Night", true));
    }

    @Override
    public EventInfo requirePublishedOrOwnedEvent(UUID eventId, UUID requesterId, boolean admin) {
        EventInfo info = events.get(eventId);
        if (info == null) {
            throw ApiException.notFound("Event not found");
        }
        return info;
    }

    @Override
    public VenueInfo requireVenueHall(UUID venueId, UUID hallId) {
        if (!VENUE.equals(venueId) || !HALL.equals(hallId)) {
            throw ApiException.unprocessable("Hall does not belong to venue");
        }
        return new VenueInfo(venueId, hallId, "Arena", "Hall A", true);
    }

    @Bean
    @Primary
    public CatalogClient catalogClient() {
        return this;
    }
}
