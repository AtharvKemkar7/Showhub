package com.eventplatform.booking.client;

import com.eventplatform.common.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.clients.stub", havingValue = "false", matchIfMissing = true)
public class RestInventoryClient implements InventoryClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.clients.inventory-base-url}")
    private String inventoryBaseUrl;

    @Value("${app.internal-token}")
    private String internalToken;

    @Override
    public SeatMap seatMap(UUID showId) {
        try {
            JsonNode node = restTemplate.getForObject(
                    inventoryBaseUrl + "/api/v1/inventory/shows/" + showId + "/seats",
                    JsonNode.class
            );
            if (node == null) {
                throw ApiException.notFound("Inventory not found");
            }
            List<SeatView> seats = new ArrayList<>();
            for (JsonNode seat : node.get("seats")) {
                seats.add(new SeatView(
                        UUID.fromString(seat.get("seatId").asText()),
                        seat.get("rowLabel").asText(),
                        seat.get("seatNumber").asInt(),
                        seat.get("seatType").asText(),
                        seat.get("state").asText(),
                        seat.hasNonNull("lockExpiresAt") ? Instant.parse(seat.get("lockExpiresAt").asText()) : null
                ));
            }
            return new SeatMap(showId, seats);
        } catch (HttpClientErrorException.NotFound ex) {
            throw ApiException.notFound("Inventory not found");
        } catch (HttpClientErrorException ex) {
            throw ApiException.unprocessable("Unable to load inventory");
        }
    }

    @Override
    public LockResult lock(UUID ownerId, UUID showId, List<UUID> seatIds, String idempotencyKey) {
        return postInternal("/api/v1/internal/locks", Map.of(
                "ownerId", ownerId,
                "showId", showId,
                "seatIds", seatIds
        ), idempotencyKey);
    }

    @Override
    public LockResult confirm(UUID lockId, UUID bookingId, String idempotencyKey) {
        return postInternal("/api/v1/internal/locks/confirm", Map.of("lockId", lockId, "bookingId", bookingId), idempotencyKey);
    }

    @Override
    public LockResult release(UUID lockId, String idempotencyKey) {
        return postInternal("/api/v1/internal/locks/release", Map.of("lockId", lockId), idempotencyKey);
    }

    @Override
    public LockResult unbook(UUID bookingId, String idempotencyKey) {
        return postInternal("/api/v1/internal/bookings/unbook", Map.of("bookingId", bookingId), idempotencyKey);
    }

    private LockResult postInternal(String path, Map<String, Object> body, String idempotencyKey) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Token", internalToken);
            if (idempotencyKey != null) {
                headers.set("Idempotency-Key", idempotencyKey);
            }
            JsonNode node = restTemplate.postForObject(inventoryBaseUrl + path, new HttpEntity<>(body, headers), JsonNode.class);
            if (node == null) {
                throw ApiException.unprocessable("Empty inventory response");
            }
            return toLock(node);
        } catch (HttpClientErrorException.Conflict ex) {
            throw ApiException.conflict("Inventory conflict");
        } catch (HttpClientErrorException.NotFound ex) {
            throw ApiException.notFound("Inventory lock not found");
        } catch (HttpClientErrorException ex) {
            throw ApiException.unprocessable("Inventory operation failed");
        }
    }

    private LockResult toLock(JsonNode node) {
        UUID lockId = node.hasNonNull("lockId") ? UUID.fromString(node.get("lockId").asText()) : null;
        UUID ownerId = node.hasNonNull("ownerId") ? UUID.fromString(node.get("ownerId").asText()) : null;
        Instant expiresAt = node.hasNonNull("expiresAt") ? Instant.parse(node.get("expiresAt").asText()) : Instant.now();
        List<UUID> seatIds = new ArrayList<>();
        if (node.has("seatIds")) {
            for (JsonNode id : node.get("seatIds")) {
                seatIds.add(UUID.fromString(id.asText()));
            }
        }
        return new LockResult(
                lockId,
                UUID.fromString(node.get("showId").asText()),
                ownerId,
                expiresAt,
                seatIds,
                node.get("status").asText()
        );
    }
}
