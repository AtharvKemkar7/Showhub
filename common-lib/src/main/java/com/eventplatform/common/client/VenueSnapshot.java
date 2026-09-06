package com.eventplatform.common.client;

import java.util.List;
import java.util.UUID;

public record VenueSnapshot(
        UUID id,
        String name,
        String city,
        boolean active,
        List<HallSnapshot> halls
) {
    public record HallSnapshot(UUID id, String name, int capacity, List<SeatSnapshot> seats) {
    }

    public record SeatSnapshot(UUID id, String rowLabel, int seatNumber, String seatType, boolean enabled) {
    }
}
