package com.eventplatform.show.client;

import com.eventplatform.common.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.clients.stub", havingValue = "false", matchIfMissing = true)
public class RestCatalogClient implements CatalogClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.clients.event-base-url}")
    private String eventBaseUrl;

    @Value("${app.clients.venue-base-url}")
    private String venueBaseUrl;

    @Override
    public EventInfo requirePublishedOrOwnedEvent(UUID eventId, UUID requesterId, boolean admin) {
        try {
            JsonNode node = restTemplate.getForObject(eventBaseUrl + "/api/v1/events/" + eventId, JsonNode.class);
            if (node == null) {
                throw ApiException.notFound("Event not found");
            }
            return toEvent(node);
        } catch (HttpClientErrorException.NotFound ex) {
            if (admin) {
                return fetchAdminEvent(eventId, requesterId);
            }
            throw ApiException.unprocessable("Event is not published or does not exist");
        } catch (HttpClientErrorException ex) {
            throw ApiException.unprocessable("Unable to validate event");
        }
    }

    private EventInfo fetchAdminEvent(UUID eventId, UUID requesterId) {
        throw ApiException.unprocessable("Event is not published or does not exist");
    }

    private EventInfo toEvent(JsonNode node) {
        return new EventInfo(
                UUID.fromString(node.get("id").asText()),
                UUID.fromString(node.get("organizerId").asText()),
                node.get("status").asText(),
                node.get("title").asText(),
                "PUBLISHED".equalsIgnoreCase(node.get("status").asText())
        );
    }

    @Override
    public VenueInfo requireVenueHall(UUID venueId, UUID hallId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            JsonNode venue = restTemplate.exchange(
                    venueBaseUrl + "/api/v1/venues/" + venueId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    JsonNode.class
            ).getBody();
            if (venue == null) {
                throw ApiException.notFound("Venue not found");
            }
            boolean hallFound = false;
            String hallName = null;
            if (venue.has("halls")) {
                for (JsonNode hall : venue.get("halls")) {
                    if (hallId.toString().equals(hall.get("id").asText())) {
                        hallFound = true;
                        hallName = hall.get("name").asText();
                    }
                }
            }
            if (!hallFound) {
                throw ApiException.unprocessable("Hall does not belong to venue");
            }
            return new VenueInfo(venueId, hallId, venue.get("name").asText(), hallName, venue.get("active").asBoolean());
        } catch (HttpClientErrorException ex) {
            throw ApiException.unprocessable("Unable to validate venue/hall");
        }
    }
}
