package com.eventplatform.show.web;

import com.eventplatform.common.security.SecurityUtils;
import com.eventplatform.show.domain.ShowStatus;
import com.eventplatform.show.dto.CreateShowRequest;
import com.eventplatform.show.dto.ShowResponse;
import com.eventplatform.show.dto.UpdateShowRequest;
import com.eventplatform.show.service.ShowManagementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizer/shows")
@PreAuthorize("hasRole('ORGANIZER')")
public class OrganizerShowController {

    private final ShowManagementService showManagementService;

    public OrganizerShowController(ShowManagementService showManagementService) {
        this.showManagementService = showManagementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShowResponse create(@Valid @RequestBody CreateShowRequest request) {
        return showManagementService.create(SecurityUtils.currentUser(), request);
    }

    @GetMapping
    public Page<ShowResponse> list(
            @RequestParam(required = false) ShowStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return showManagementService.search(null, null, status, SecurityUtils.currentUserId(), pageable);
    }

    @GetMapping("/{showId}")
    public ShowResponse get(@PathVariable UUID showId) {
        return showManagementService.get(SecurityUtils.currentUser(), showId);
    }

    @PatchMapping("/{showId}")
    public ShowResponse update(@PathVariable UUID showId, @Valid @RequestBody UpdateShowRequest request) {
        return showManagementService.update(SecurityUtils.currentUser(), showId, request);
    }

    @DeleteMapping("/{showId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID showId) {
        showManagementService.delete(SecurityUtils.currentUser(), showId);
    }
}
