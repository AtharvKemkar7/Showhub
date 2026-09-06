package com.eventplatform.booking.client;

import java.util.List;
import java.util.UUID;

public interface ShowCatalogClient {

    ShowInfo requireBookableShow(UUID showId);

    record ShowInfo(
            UUID id,
            UUID eventId,
            UUID organizerId,
            UUID venueId,
            UUID hallId,
            String status,
            List<Price> prices
    ) {
        public record Price(String seatType, int amountCents, String currency) {
        }

        public Price priceFor(String seatType) {
            return prices.stream()
                    .filter(p -> p.seatType().equalsIgnoreCase(seatType))
                    .findFirst()
                    .orElse(null);
        }
    }
}
