package com.eventplatform.search.service;

import com.eventplatform.common.messaging.DomainEvent;
import com.eventplatform.search.domain.SearchEvent;
import com.eventplatform.search.dto.IndexEventRequest;
import com.eventplatform.search.dto.SearchEventResponse;
import com.eventplatform.search.repository.SearchEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
public class EventIndexService {

    private static final Logger log = LoggerFactory.getLogger(EventIndexService.class);

    private final SearchEventRepository searchEventRepository;
    private final ObjectMapper objectMapper;

    public EventIndexService(SearchEventRepository searchEventRepository, ObjectMapper objectMapper) {
        this.searchEventRepository = searchEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SearchEventResponse index(IndexEventRequest request) {
        SearchEvent event = searchEventRepository.findById(request.eventId()).orElseGet(SearchEvent::new);
        event.setEventId(request.eventId());
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setCity(request.city());
        event.setCategoryName(request.categoryName());
        event.setStatus(request.status());
        event.setOrganizerId(request.organizerId());
        event.setStartAt(request.startAt());
        searchEventRepository.save(event);
        return SearchEventResponse.from(event);
    }

    @Transactional
    public void consume(DomainEvent event) {
        if (event == null || !StringUtils.hasText(event.payloadJson())) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(event.payloadJson());
            UUID eventId = node.hasNonNull("id")
                    ? UUID.fromString(node.get("id").asText())
                    : UUID.fromString(event.aggregateId());
            String status = node.hasNonNull("status") ? node.get("status").asText() : "PUBLISHED";
            if ("CANCELLED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status)) {
                searchEventRepository.deleteById(eventId);
                return;
            }
            if (!"PUBLISHED".equalsIgnoreCase(status) && !"event.published".equalsIgnoreCase(event.type())) {
                return;
            }
            index(new IndexEventRequest(
                    eventId,
                    node.path("title").asText("Untitled"),
                    node.path("description").asText(null),
                    node.path("city").asText(null),
                    node.has("category") ? node.path("category").path("name").asText(null) : node.path("categoryName").asText(null),
                    status,
                    node.hasNonNull("organizerId") ? UUID.fromString(node.get("organizerId").asText()) : event.actorId(),
                    node.hasNonNull("startAt") ? Instant.parse(node.get("startAt").asText()) : null
            ));
        } catch (Exception ex) {
            log.warn("Failed to index event type={} aggregate={}", event.type(), event.aggregateId());
        }
    }

    @Transactional(readOnly = true)
    public Page<SearchEventResponse> search(String q, String city, String category, Instant from, Instant to, Pageable pageable) {
        String query = StringUtils.hasText(q) ? q.trim() : null;
        String cityFilter = StringUtils.hasText(city) ? city.trim() : null;
        String categoryFilter = StringUtils.hasText(category) ? category.trim() : null;
        return searchEventRepository.search(query, cityFilter, categoryFilter, from, to, pageable)
                .map(SearchEventResponse::from);
    }
}
