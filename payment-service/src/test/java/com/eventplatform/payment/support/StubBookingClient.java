package com.eventplatform.payment.support;

import com.eventplatform.payment.client.BookingClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@TestConfiguration
public class StubBookingClient {

    public static final AtomicInteger CONFIRMS = new AtomicInteger();
    public static final ConcurrentHashMap<UUID, UUID> CONFIRMED = new ConcurrentHashMap<>();

    @Bean
    @Primary
    public BookingClient bookingClient() {
        return (bookingId, paymentId, idempotencyKey) -> {
            CONFIRMS.incrementAndGet();
            CONFIRMED.put(bookingId, paymentId);
        };
    }
}
