package com.eventplatform.payment.client;

import com.eventplatform.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.clients.stub", havingValue = "false", matchIfMissing = true)
public class RestBookingClient implements BookingClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.clients.booking-base-url}")
    private String bookingBaseUrl;

    @Value("${app.internal-token}")
    private String internalToken;

    @Override
    public void confirm(UUID bookingId, UUID paymentId, String idempotencyKey) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Token", internalToken);
            if (idempotencyKey != null) {
                headers.set("Idempotency-Key", idempotencyKey);
            }
            restTemplate.postForObject(
                    bookingBaseUrl + "/api/v1/internal/bookings/" + bookingId + "/confirm",
                    new HttpEntity<>(Map.of("paymentId", paymentId), headers),
                    String.class
            );
        } catch (HttpClientErrorException.Conflict ex) {
            throw ApiException.conflict("Booking cannot be confirmed");
        } catch (HttpClientErrorException.NotFound ex) {
            throw ApiException.notFound("Booking not found");
        } catch (HttpClientErrorException ex) {
            throw ApiException.unprocessable("Unable to confirm booking");
        }
    }
}
