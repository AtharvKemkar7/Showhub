package com.eventplatform.show.web;

import com.eventplatform.show.domain.ShowStatus;
import com.eventplatform.show.dto.ShowResponse;
import com.eventplatform.show.service.ShowManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shows")
public class PublicShowController {

    private final ShowManagementService showManagementService;

    public PublicShowController(ShowManagementService showManagementService) {
        this.showManagementService = showManagementService;
    }

    @GetMapping
    public Page<ShowResponse> search(
            @RequestParam(required = false) UUID eventId,
            @RequestParam(required = false) UUID venueId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return showManagementService.search(eventId, venueId, ShowStatus.SCHEDULED, null, pageable);
    }

    @GetMapping("/{showId}")
    public ShowResponse get(@PathVariable UUID showId) {
        return showManagementService.get(null, showId);
    }
}
