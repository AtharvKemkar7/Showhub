package com.eventplatform.booking.support;

import com.eventplatform.booking.client.InventoryClient;
import com.eventplatform.booking.client.ShowCatalogClient;
import com.eventplatform.common.exception.ApiException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@TestConfiguration
public class StubClients {

    public static final UUID SHOW_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID EVENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID ORGANIZER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID VENUE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final UUID HALL_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    public static final UUID SEAT_A = UUID.fromString("66666666-6666-6666-6666-666666666666");
    public static final UUID SEAT_B = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @Bean
    @Primary
    public ShowCatalogClient showCatalogClient() {
        return showId -> {
            if (!SHOW_ID.equals(showId)) {
                throw ApiException.notFound("Show not found");
            }
            return new ShowCatalogClient.ShowInfo(
                    SHOW_ID,
                    EVENT_ID,
                    ORGANIZER_ID,
                    VENUE_ID,
                    HALL_ID,
                    "SCHEDULED",
                    List.of(
                            new ShowCatalogClient.ShowInfo.Price("REGULAR", 50000, "INR"),
                            new ShowCatalogClient.ShowInfo.Price("PREMIUM", 80000, "INR")
                    )
            );
        };
    }

    @Bean
    @Primary
    public InMemoryInventoryClient inventoryClient() {
        return new InMemoryInventoryClient();
    }

    public static class InMemoryInventoryClient implements InventoryClient {
        private final Map<UUID, LockResult> locks = new ConcurrentHashMap<>();
        private final Map<UUID, UUID> booked = new ConcurrentHashMap<>();

        public void reset() {
            locks.clear();
            booked.clear();
        }

        @Override
        public SeatMap seatMap(UUID showId) {
            return new SeatMap(showId, List.of(
                    new SeatView(SEAT_A, "A", 1, "REGULAR", "AVAILABLE", null),
                    new SeatView(SEAT_B, "A", 2, "PREMIUM", "AVAILABLE", null)
            ));
        }

        @Override
        public synchronized LockResult lock(UUID ownerId, UUID showId, List<UUID> seatIds, String idempotencyKey) {
            for (UUID seatId : seatIds) {
                if (booked.containsKey(seatId) || locks.values().stream().anyMatch(l -> l.seatIds().contains(seatId))) {
                    throw ApiException.conflict("Seat is already locked");
                }
            }
            LockResult result = new LockResult(
                    UUID.randomUUID(),
                    showId,
                    ownerId,
                    Instant.now().plus(5, ChronoUnit.MINUTES),
                    seatIds,
                    "LOCKED"
            );
            locks.put(result.lockId(), result);
            return result;
        }

        @Override
        public synchronized LockResult confirm(UUID lockId, UUID bookingId, String idempotencyKey) {
            LockResult lock = locks.remove(lockId);
            if (lock == null) {
                throw ApiException.notFound("Lock not found");
            }
            lock.seatIds().forEach(id -> booked.put(id, bookingId));
            return new LockResult(lockId, lock.showId(), lock.ownerId(), Instant.now(), lock.seatIds(), "BOOKED");
        }

        @Override
        public synchronized LockResult release(UUID lockId, String idempotencyKey) {
            LockResult lock = locks.remove(lockId);
            if (lock == null) {
                throw ApiException.notFound("Lock not found");
            }
            return new LockResult(lockId, lock.showId(), lock.ownerId(), Instant.now(), lock.seatIds(), "RELEASED");
        }

        @Override
        public synchronized LockResult unbook(UUID bookingId, String idempotencyKey) {
            List<UUID> seats = booked.entrySet().stream()
                    .filter(e -> bookingId.equals(e.getValue()))
                    .map(Map.Entry::getKey)
                    .toList();
            seats.forEach(booked::remove);
            return new LockResult(null, SHOW_ID, null, Instant.now(), seats, "AVAILABLE");
        }
    }
}
