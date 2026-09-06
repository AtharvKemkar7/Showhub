package com.eventplatform.inventory.web;

import com.eventplatform.common.security.SecurityUtils;
import com.eventplatform.inventory.dto.ConfirmLockRequest;
import com.eventplatform.inventory.dto.CreateInventoryRequest;
import com.eventplatform.inventory.dto.LockCommandRequest;
import com.eventplatform.inventory.dto.LockResponse;
import com.eventplatform.inventory.dto.LockSeatsRequest;
import com.eventplatform.inventory.dto.SeatMapResponse;
import com.eventplatform.inventory.service.SeatLockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class InventoryController {

    private final SeatLockService seatLockService;

    public InventoryController(SeatLockService seatLockService) {
        this.seatLockService = seatLockService;
    }

    @PostMapping("/inventory/shows")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public SeatMapResponse create(@Valid @RequestBody CreateInventoryRequest request) {
        return seatLockService.createInventory(request);
    }

    @GetMapping("/inventory/shows/{showId}/seats")
    public SeatMapResponse seatMap(@PathVariable UUID showId) {
        return seatLockService.seatMap(showId);
    }

    @PostMapping("/inventory/locks")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public LockResponse lock(
            @Valid @RequestBody LockSeatsRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return seatLockService.lock(SecurityUtils.currentUserId(), request, idempotencyKey);
    }

    @PostMapping("/inventory/locks/release")
    @PreAuthorize("isAuthenticated()")
    public LockResponse release(
            @Valid @RequestBody LockCommandRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        var user = SecurityUtils.currentUser();
        return seatLockService.release(user.getId(), request.lockId(), user.isAdmin(), idempotencyKey);
    }

    @PostMapping("/inventory/locks/extend")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public LockResponse extend(
            @Valid @RequestBody LockCommandRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return seatLockService.extend(SecurityUtils.currentUserId(), request.lockId(), idempotencyKey);
    }

    @PostMapping("/inventory/locks/confirm")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public LockResponse confirm(
            @Valid @RequestBody ConfirmLockRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return seatLockService.confirm(SecurityUtils.currentUserId(), request, false, idempotencyKey);
    }
}
