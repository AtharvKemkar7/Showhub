package com.eventplatform.venue.web;

import com.eventplatform.venue.dto.HallResponse;
import com.eventplatform.venue.dto.VenueResponse;
import com.eventplatform.venue.service.VenueService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues")
@PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
public class OrganizerVenueController {

    private final VenueService venueService;

    public OrganizerVenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping
    public Page<VenueResponse> list(
            @RequestParam(required = false) String city,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return venueService.listAvailable(city, pageable);
    }

    @GetMapping("/{venueId}")
    public VenueResponse get(@PathVariable UUID venueId) {
        return venueService.getVenue(venueId, true);
    }

    @GetMapping("/{venueId}/halls/{hallId}")
    public HallResponse getHall(@PathVariable UUID venueId, @PathVariable UUID hallId) {
        return venueService.getHall(venueId, hallId);
    }
}
