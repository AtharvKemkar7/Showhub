package com.eventplatform.booking.web;

import com.eventplatform.booking.dto.BookingResponse;
import com.eventplatform.booking.dto.ConfirmPaymentRequest;
import com.eventplatform.booking.service.BookingManagementService;
import com.eventplatform.common.security.InternalTokenValidator;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal")
public class InternalBookingController {

    private final BookingManagementService bookingManagementService;
    private final InternalTokenValidator internalTokenValidator;

    public InternalBookingController(
            BookingManagementService bookingManagementService,
            InternalTokenValidator internalTokenValidator
    ) {
        this.bookingManagementService = bookingManagementService;
        this.internalTokenValidator = internalTokenValidator;
    }

    @PostMapping("/bookings/{bookingId}/confirm")
    public BookingResponse confirm(
            @RequestHeader("X-Internal-Token") String internalToken,
            @PathVariable UUID bookingId,
            @Valid @RequestBody ConfirmPaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        internalTokenValidator.require(internalToken);
        return bookingManagementService.confirmPayment(bookingId, request.paymentId(), idempotencyKey);
    }
}
