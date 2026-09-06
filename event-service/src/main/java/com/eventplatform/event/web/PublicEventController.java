package com.eventplatform.event.web;

import com.eventplatform.event.dto.EventCategoryResponse;
import com.eventplatform.event.dto.EventResponse;
import com.eventplatform.event.service.CategoryService;
import com.eventplatform.event.service.EventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PublicEventController {

    private final EventService eventService;
    private final CategoryService categoryService;

    public PublicEventController(EventService eventService, CategoryService categoryService) {
        this.eventService = eventService;
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public List<EventCategoryResponse> categories() {
        return categoryService.list();
    }

    @GetMapping("/categories/{categoryId}")
    public EventCategoryResponse category(@PathVariable UUID categoryId) {
        return categoryService.get(categoryId);
    }

    @GetMapping("/events")
    public Page<EventResponse> search(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return eventService.searchPublic(categoryId, city, title, from, to, pageable);
    }

    @GetMapping("/events/{eventId}")
    public EventResponse get(@PathVariable UUID eventId) {
        return eventService.getPublic(eventId);
    }
}
