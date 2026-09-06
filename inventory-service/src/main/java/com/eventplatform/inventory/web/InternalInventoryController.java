package com.eventplatform.inventory.web;

import com.eventplatform.common.security.InternalTokenValidator;
import com.eventplatform.inventory.dto.ConfirmLockRequest;
import com.eventplatform.inventory.dto.InternalLockRequest;
import com.eventplatform.inventory.dto.LockCommandRequest;
import com.eventplatform.inventory.dto.LockResponse;
import com.eventplatform.inventory.dto.LockSeatsRequest;
import com.eventplatform.inventory.dto.UnbookRequest;
import com.eventplatform.inventory.service.SeatLockService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal")
public class InternalInventoryController {

    private final SeatLockService seatLockService;
    private final InternalTokenValidator internalTokenValidator;

    public InternalInventoryController(SeatLockService seatLockService, InternalTokenValidator internalTokenValidator) {
        this.seatLockService = seatLockService;
        this.internalTokenValidator = internalTokenValidator;
    }

    @PostMapping("/locks")
    public LockResponse lock(
            @RequestHeader("X-Internal-Token") String internalToken,
            @Valid @RequestBody InternalLockRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        internalTokenValidator.require(internalToken);
        return seatLockService.lock(
                request.ownerId(),
                new LockSeatsRequest(request.showId(), request.seatIds()),
                idempotencyKey
        );
    }

    @PostMapping("/locks/confirm")
    public LockResponse confirm(
            @RequestHeader("X-Internal-Token") String internalToken,
            @Valid @RequestBody ConfirmLockRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        internalTokenValidator.require(internalToken);
        return seatLockService.confirm(request.bookingId(), request, true, idempotencyKey);
    }

    @PostMapping("/locks/release")
    public LockResponse release(
            @RequestHeader("X-Internal-Token") String internalToken,
            @Valid @RequestBody LockCommandRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        internalTokenValidator.require(internalToken);
        return seatLockService.release(null, request.lockId(), true, idempotencyKey);
    }

    @PostMapping("/bookings/unbook")
    public LockResponse unbook(
            @RequestHeader("X-Internal-Token") String internalToken,
            @Valid @RequestBody UnbookRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        internalTokenValidator.require(internalToken);
        return seatLockService.unbook(request.bookingId(), idempotencyKey);
    }
}
