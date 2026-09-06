package com.eventplatform.booking.web;

import com.eventplatform.booking.dto.BookingResponse;
import com.eventplatform.booking.dto.CancelBookingRequest;
import com.eventplatform.booking.dto.CreateBookingRequest;
import com.eventplatform.booking.service.BookingManagementService;
import com.eventplatform.common.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
public class BookingController {

    private final BookingManagementService bookingManagementService;

    public BookingController(BookingManagementService bookingManagementService) {
        this.bookingManagementService = bookingManagementService;
    }

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public BookingResponse create(
            @Valid @RequestBody CreateBookingRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return bookingManagementService.create(SecurityUtils.currentUser(), request, idempotencyKey);
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public Page<BookingResponse> mine(@PageableDefault(size = 20) Pageable pageable) {
        var user = SecurityUtils.currentUser();
        if (user.isAdmin()) {
            return bookingManagementService.admin(pageable);
        }
        return bookingManagementService.mine(user.getId(), pageable);
    }

    @GetMapping("/bookings/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public BookingResponse get(@PathVariable UUID bookingId) {
        return bookingManagementService.get(SecurityUtils.currentUser(), bookingId);
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public BookingResponse cancel(
            @PathVariable UUID bookingId,
            @RequestBody(required = false) CancelBookingRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        String reason = request == null ? null : request.reason();
        return bookingManagementService.cancel(SecurityUtils.currentUser(), bookingId, reason, idempotencyKey);
    }

    @GetMapping("/organizer/bookings")
    @PreAuthorize("hasRole('ORGANIZER')")
    public Page<BookingResponse> organizer(@PageableDefault(size = 20) Pageable pageable) {
        return bookingManagementService.organizer(SecurityUtils.currentUserId(), pageable);
    }

    @GetMapping("/admin/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<BookingResponse> admin(@PageableDefault(size = 20) Pageable pageable) {
        return bookingManagementService.admin(pageable);
    }
}
