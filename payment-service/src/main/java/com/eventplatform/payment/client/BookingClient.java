package com.eventplatform.payment.client;

import java.util.UUID;

public interface BookingClient {

    void confirm(UUID bookingId, UUID paymentId, String idempotencyKey);
}
