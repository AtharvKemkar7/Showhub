package com.eventplatform.booking.client;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface InventoryClient {

    SeatMap seatMap(UUID showId);

    LockResult lock(UUID ownerId, UUID showId, List<UUID> seatIds, String idempotencyKey);

    LockResult confirm(UUID lockId, UUID bookingId, String idempotencyKey);

    LockResult release(UUID lockId, String idempotencyKey);

    LockResult unbook(UUID bookingId, String idempotencyKey);

    record SeatMap(UUID showId, List<SeatView> seats) {
        public SeatView seat(UUID seatId) {
            return seats.stream().filter(s -> s.seatId().equals(seatId)).findFirst().orElse(null);
        }
    }

    record SeatView(UUID seatId, String rowLabel, int seatNumber, String seatType, String state, Instant lockExpiresAt) {
    }

    record LockResult(UUID lockId, UUID showId, UUID ownerId, Instant expiresAt, List<UUID> seatIds, String status) {
    }
}
