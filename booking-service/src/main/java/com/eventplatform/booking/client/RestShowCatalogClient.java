package com.eventplatform.booking.client;

import com.eventplatform.common.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.clients.stub", havingValue = "false", matchIfMissing = true)
public class RestShowCatalogClient implements ShowCatalogClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.clients.show-base-url}")
    private String showBaseUrl;

    @Override
    public ShowInfo requireBookableShow(UUID showId) {
        try {
            JsonNode node = restTemplate.getForObject(showBaseUrl + "/api/v1/shows/" + showId, JsonNode.class);
            if (node == null) {
                throw ApiException.notFound("Show not found");
            }
            String status = node.get("status").asText();
            if (!"SCHEDULED".equalsIgnoreCase(status) && !"ACTIVE".equalsIgnoreCase(status)) {
                throw ApiException.unprocessable("Show is not bookable");
            }
            List<ShowInfo.Price> prices = new ArrayList<>();
            if (node.has("prices")) {
                for (JsonNode price : node.get("prices")) {
                    prices.add(new ShowInfo.Price(
                            price.get("seatType").asText(),
                            price.get("amountCents").asInt(),
                            price.has("currency") ? price.get("currency").asText() : "INR"
                    ));
                }
            }
            return new ShowInfo(
                    UUID.fromString(node.get("id").asText()),
                    UUID.fromString(node.get("eventId").asText()),
                    UUID.fromString(node.get("organizerId").asText()),
                    UUID.fromString(node.get("venueId").asText()),
                    UUID.fromString(node.get("hallId").asText()),
                    status,
                    prices
            );
        } catch (HttpClientErrorException.NotFound ex) {
            throw ApiException.notFound("Show not found");
        } catch (HttpClientErrorException ex) {
            throw ApiException.unprocessable("Unable to validate show");
        }
    }
}
