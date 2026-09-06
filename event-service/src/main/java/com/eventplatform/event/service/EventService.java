package com.eventplatform.event.service;

import com.eventplatform.event.domain.Event;
import com.eventplatform.event.domain.EventCategory;
import com.eventplatform.event.domain.EventStatus;
import com.eventplatform.event.dto.CreateEventRequest;
import com.eventplatform.event.dto.EventResponse;
import com.eventplatform.event.dto.RejectEventRequest;
import com.eventplatform.event.dto.UpdateEventRequest;
import com.eventplatform.event.exception.ApiException;
import com.eventplatform.event.repository.EventRepository;
import com.eventplatform.event.repository.EventSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final CategoryService categoryService;

    public EventService(EventRepository eventRepository, CategoryService categoryService) {
        this.eventRepository = eventRepository;
        this.categoryService = categoryService;
    }

    @Transactional
    public EventResponse create(UUID organizerId, CreateEventRequest request) {
        validateDates(request.startAt(), request.endAt());
        EventCategory category = categoryService.require(request.categoryId());
        Event event = new Event();
        event.setOrganizerId(organizerId);
        event.setCategory(category);
        event.setTitle(request.title().trim());
        event.setDescription(request.description().trim());
        event.setVenueName(request.venueName().trim());
        event.setVenueAddress(request.venueAddress().trim());
        event.setCity(request.city().trim());
        event.setStartAt(request.startAt());
        event.setEndAt(request.endAt());
        event.setStatus(EventStatus.DRAFT);
        eventRepository.save(event);
        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse updateOwned(UUID organizerId, UUID eventId, UpdateEventRequest request) {
        Event event = requireOwned(organizerId, eventId);
        assertModifiable(event);
        applyUpdates(event, request);
        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse submit(UUID organizerId, UUID eventId) {
        Event event = requireOwned(organizerId, eventId);
        if (event.getStatus() != EventStatus.DRAFT && event.getStatus() != EventStatus.REJECTED) {
            throw ApiException.conflict("Only draft or rejected events can be submitted for approval");
        }
        event.setStatus(EventStatus.PENDING_APPROVAL);
        event.setRejectionReason(null);
        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse cancelOwned(UUID organizerId, UUID eventId) {
        Event event = requireOwned(organizerId, eventId);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw ApiException.conflict("Event is already cancelled");
        }
        event.setStatus(EventStatus.CANCELLED);
        return EventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> listOwned(UUID organizerId, EventStatus status, UUID categoryId, String city, String title, Instant from, Instant to, Pageable pageable) {
        return eventRepository.findAll(
                EventSpecifications.filter(status, categoryId, city, title, from, to, organizerId),
                pageable
        ).map(EventResponse::from);
    }

    @Transactional(readOnly = true)
    public EventResponse getOwned(UUID organizerId, UUID eventId) {
        return EventResponse.from(requireOwned(organizerId, eventId));
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> searchPublic(UUID categoryId, String city, String title, Instant from, Instant to, Pageable pageable) {
        return eventRepository.findAll(
                EventSpecifications.filter(EventStatus.PUBLISHED, categoryId, city, title, from, to, null),
                pageable
        ).map(EventResponse::from);
    }

    @Transactional(readOnly = true)
    public EventResponse getPublic(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound("Event not found"));
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw ApiException.notFound("Event not found");
        }
        return EventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> adminSearch(EventStatus status, UUID categoryId, UUID organizerId, String city, String title, Instant from, Instant to, Pageable pageable) {
        return eventRepository.findAll(
                EventSpecifications.filter(status, categoryId, city, title, from, to, organizerId),
                pageable
        ).map(EventResponse::from);
    }

    @Transactional(readOnly = true)
    public EventResponse adminGet(UUID eventId) {
        return EventResponse.from(requireEvent(eventId));
    }

    @Transactional
    public EventResponse approve(UUID eventId) {
        Event event = requireEvent(eventId);
        if (event.getStatus() != EventStatus.PENDING_APPROVAL) {
            throw ApiException.conflict("Only events pending approval can be published");
        }
        event.setStatus(EventStatus.PUBLISHED);
        event.setRejectionReason(null);
        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse reject(UUID eventId, RejectEventRequest request) {
        Event event = requireEvent(eventId);
        if (event.getStatus() != EventStatus.PENDING_APPROVAL) {
            throw ApiException.conflict("Only events pending approval can be rejected");
        }
        event.setStatus(EventStatus.REJECTED);
        event.setRejectionReason(request.reason().trim());
        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse adminCancel(UUID eventId) {
        Event event = requireEvent(eventId);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw ApiException.conflict("Event is already cancelled");
        }
        event.setStatus(EventStatus.CANCELLED);
        return EventResponse.from(event);
    }

    private void applyUpdates(Event event, UpdateEventRequest request) {
        Instant startAt = request.startAt() != null ? request.startAt() : event.getStartAt();
        Instant endAt = request.endAt() != null ? request.endAt() : event.getEndAt();
        validateDates(startAt, endAt);
        if (request.categoryId() != null) {
            event.setCategory(categoryService.require(request.categoryId()));
        }
        if (StringUtils.hasText(request.title())) {
            event.setTitle(request.title().trim());
        }
        if (StringUtils.hasText(request.description())) {
            event.setDescription(request.description().trim());
        }
        if (StringUtils.hasText(request.venueName())) {
            event.setVenueName(request.venueName().trim());
        }
        if (StringUtils.hasText(request.venueAddress())) {
            event.setVenueAddress(request.venueAddress().trim());
        }
        if (StringUtils.hasText(request.city())) {
            event.setCity(request.city().trim());
        }
        event.setStartAt(startAt);
        event.setEndAt(endAt);
    }

    private void validateDates(Instant startAt, Instant endAt) {
        if (startAt == null || endAt == null) {
            throw ApiException.badRequest("Start and end times are required");
        }
        if (!endAt.isAfter(startAt)) {
            throw ApiException.badRequest("End time must be after start time");
        }
    }

    private void assertModifiable(Event event) {
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.PUBLISHED) {
            throw ApiException.conflict("Published or cancelled events cannot be modified");
        }
        if (event.getStatus() == EventStatus.PENDING_APPROVAL) {
            throw ApiException.conflict("Events pending approval cannot be modified");
        }
    }

    private Event requireOwned(UUID organizerId, UUID eventId) {
        return eventRepository.findByIdAndOrganizerId(eventId, organizerId)
                .orElseThrow(() -> ApiException.notFound("Event not found"));
    }

    private Event requireEvent(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound("Event not found"));
    }
}
