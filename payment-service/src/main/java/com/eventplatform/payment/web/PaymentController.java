package com.eventplatform.payment.web;

import com.eventplatform.common.security.SecurityUtils;
import com.eventplatform.payment.dto.InitiatePaymentRequest;
import com.eventplatform.payment.dto.PaymentResponse;
import com.eventplatform.payment.dto.RefundRequest;
import com.eventplatform.payment.dto.WebhookRequest;
import com.eventplatform.payment.service.PaymentManagementService;
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
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentManagementService paymentManagementService;

    public PaymentController(PaymentManagementService paymentManagementService) {
        this.paymentManagementService = paymentManagementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public PaymentResponse initiate(
            @Valid @RequestBody InitiatePaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return paymentManagementService.initiate(SecurityUtils.currentUser(), request, idempotencyKey);
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    public PaymentResponse get(@PathVariable UUID paymentId) {
        return paymentManagementService.get(SecurityUtils.currentUser(), paymentId);
    }

    @PostMapping("/webhook")
    public PaymentResponse webhook(
            @RequestHeader("X-Webhook-Secret") String secret,
            @Valid @RequestBody WebhookRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return paymentManagementService.webhook(secret, request, idempotencyKey);
    }

    @PostMapping("/{paymentId}/mock-checkout")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public PaymentResponse mockCheckout(@PathVariable UUID paymentId) {
        return paymentManagementService.mockSucceed(paymentId);
    }

    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public PaymentResponse refund(
            @PathVariable UUID paymentId,
            @RequestBody(required = false) RefundRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        String reason = request == null ? null : request.reason();
        return paymentManagementService.refund(SecurityUtils.currentUser(), paymentId, reason, idempotencyKey);
    }
}
