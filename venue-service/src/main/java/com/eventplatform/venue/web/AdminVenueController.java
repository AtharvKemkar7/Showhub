package com.eventplatform.venue.web;

import com.eventplatform.venue.dto.BulkSeatLayoutRequest;
import com.eventplatform.venue.dto.CreateHallRequest;
import com.eventplatform.venue.dto.CreateSeatRequest;
import com.eventplatform.venue.dto.CreateVenueRequest;
import com.eventplatform.venue.dto.HallResponse;
import com.eventplatform.venue.dto.SeatResponse;
import com.eventplatform.venue.dto.UpdateSeatRequest;
import com.eventplatform.venue.dto.UpdateVenueRequest;
import com.eventplatform.venue.dto.VenueResponse;
import com.eventplatform.venue.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/venues")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVenueController {

    private final VenueService venueService;

    public AdminVenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VenueResponse create(@Valid @RequestBody CreateVenueRequest request) {
        return venueService.createVenue(request);
    }

    @GetMapping
    public Page<VenueResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return venueService.listAdmin(pageable);
    }

    @GetMapping("/{venueId}")
    public VenueResponse get(@PathVariable UUID venueId) {
        return venueService.getVenue(venueId, false);
    }

    @PatchMapping("/{venueId}")
    public VenueResponse update(@PathVariable UUID venueId, @Valid @RequestBody UpdateVenueRequest request) {
        return venueService.updateVenue(venueId, request);
    }

    @PostMapping("/{venueId}/halls")
    @ResponseStatus(HttpStatus.CREATED)
    public HallResponse createHall(@PathVariable UUID venueId, @Valid @RequestBody CreateHallRequest request) {
        return venueService.createHall(venueId, request);
    }

    @GetMapping("/{venueId}/halls/{hallId}")
    public HallResponse getHall(@PathVariable UUID venueId, @PathVariable UUID hallId) {
        return venueService.getHall(venueId, hallId);
    }

    @PostMapping("/{venueId}/halls/{hallId}/seats")
    @ResponseStatus(HttpStatus.CREATED)
    public SeatResponse addSeat(
            @PathVariable UUID venueId,
            @PathVariable UUID hallId,
            @Valid @RequestBody CreateSeatRequest request
    ) {
        return venueService.addSeat(venueId, hallId, request);
    }

    @PostMapping("/{venueId}/halls/{hallId}/layout")
    @ResponseStatus(HttpStatus.CREATED)
    public List<SeatResponse> layout(
            @PathVariable UUID venueId,
            @PathVariable UUID hallId,
            @Valid @RequestBody BulkSeatLayoutRequest request
    ) {
        return venueService.configureLayout(venueId, hallId, request);
    }

    @PatchMapping("/{venueId}/halls/{hallId}/seats/{seatId}")
    public SeatResponse updateSeat(
            @PathVariable UUID venueId,
            @PathVariable UUID hallId,
            @PathVariable UUID seatId,
            @Valid @RequestBody UpdateSeatRequest request
    ) {
        return venueService.updateSeat(venueId, hallId, seatId, request);
    }
}
