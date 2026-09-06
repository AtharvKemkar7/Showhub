package com.eventplatform.search.web;

import com.eventplatform.search.dto.SearchEventResponse;
import com.eventplatform.search.service.EventIndexService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final EventIndexService eventIndexService;

    public SearchController(EventIndexService eventIndexService) {
        this.eventIndexService = eventIndexService;
    }

    @GetMapping("/events")
    public Page<SearchEventResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return eventIndexService.search(q, city, category, from, to, pageable);
    }
}
