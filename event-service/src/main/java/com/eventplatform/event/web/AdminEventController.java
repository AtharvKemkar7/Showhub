package com.eventplatform.event.web;

import com.eventplatform.event.domain.EventStatus;
import com.eventplatform.event.dto.CreateCategoryRequest;
import com.eventplatform.event.dto.EventCategoryResponse;
import com.eventplatform.event.dto.EventResponse;
import com.eventplatform.event.dto.RejectEventRequest;
import com.eventplatform.event.service.CategoryService;
import com.eventplatform.event.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEventController {

    private final EventService eventService;
    private final CategoryService categoryService;

    public AdminEventController(EventService eventService, CategoryService categoryService) {
        this.eventService = eventService;
        this.categoryService = categoryService;
    }

    @GetMapping("/events")
    public Page<EventResponse> search(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID organizerId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return eventService.adminSearch(status, categoryId, organizerId, city, title, from, to, pageable);
    }

    @GetMapping("/events/{eventId}")
    public EventResponse get(@PathVariable UUID eventId) {
        return eventService.adminGet(eventId);
    }

    @PostMapping("/events/{eventId}/approve")
    public EventResponse approve(@PathVariable UUID eventId) {
        return eventService.approve(eventId);
    }

    @PostMapping("/events/{eventId}/reject")
    public EventResponse reject(@PathVariable UUID eventId, @Valid @RequestBody RejectEventRequest request) {
        return eventService.reject(eventId, request);
    }

    @PostMapping("/events/{eventId}/cancel")
    public EventResponse cancel(@PathVariable UUID eventId) {
        return eventService.adminCancel(eventId);
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public EventCategoryResponse createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.create(request);
    }
}
