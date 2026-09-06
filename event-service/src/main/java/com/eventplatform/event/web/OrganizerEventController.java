package com.eventplatform.event.web;

import com.eventplatform.event.domain.EventStatus;
import com.eventplatform.event.dto.CreateEventRequest;
import com.eventplatform.event.dto.EventResponse;
import com.eventplatform.event.dto.UpdateEventRequest;
import com.eventplatform.event.security.SecurityUtils;
import com.eventplatform.event.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizer/events")
@PreAuthorize("hasRole('ORGANIZER')")
public class OrganizerEventController {

    private final EventService eventService;

    public OrganizerEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@Valid @RequestBody CreateEventRequest request) {
        return eventService.create(SecurityUtils.currentUserId(), request);
    }

    @GetMapping
    public Page<EventResponse> list(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return eventService.listOwned(SecurityUtils.currentUserId(), status, categoryId, city, title, from, to, pageable);
    }

    @GetMapping("/{eventId}")
    public EventResponse get(@PathVariable UUID eventId) {
        return eventService.getOwned(SecurityUtils.currentUserId(), eventId);
    }

    @PatchMapping("/{eventId}")
    public EventResponse update(@PathVariable UUID eventId, @Valid @RequestBody UpdateEventRequest request) {
        return eventService.updateOwned(SecurityUtils.currentUserId(), eventId, request);
    }

    @PostMapping("/{eventId}/submit")
    public EventResponse submit(@PathVariable UUID eventId) {
        return eventService.submit(SecurityUtils.currentUserId(), eventId);
    }

    @PostMapping("/{eventId}/cancel")
    public EventResponse cancel(@PathVariable UUID eventId) {
        return eventService.cancelOwned(SecurityUtils.currentUserId(), eventId);
    }
}
