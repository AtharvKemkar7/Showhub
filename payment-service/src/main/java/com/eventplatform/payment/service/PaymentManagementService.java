package com.eventplatform.payment.service;

import com.eventplatform.common.exception.ApiException;
import com.eventplatform.common.kafka.Topics;
import com.eventplatform.common.messaging.DomainEvent;
import com.eventplatform.common.messaging.EventPublisher;
import com.eventplatform.common.security.AuthenticatedUser;
import com.eventplatform.payment.client.BookingClient;
import com.eventplatform.payment.domain.Payment;
import com.eventplatform.payment.domain.PaymentCommand;
import com.eventplatform.payment.domain.PaymentStatus;
import com.eventplatform.payment.dto.InitiatePaymentRequest;
import com.eventplatform.payment.dto.PaymentResponse;
import com.eventplatform.payment.dto.WebhookRequest;
import com.eventplatform.payment.repository.PaymentCommandRepository;
import com.eventplatform.payment.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentManagementService {

    private final PaymentRepository paymentRepository;
    private final PaymentCommandRepository commandRepository;
    private final BookingClient bookingClient;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${app.webhook-secret}")
    private String webhookSecret;

    public PaymentManagementService(
            PaymentRepository paymentRepository,
            PaymentCommandRepository commandRepository,
            BookingClient bookingClient,
            EventPublisher eventPublisher,
            ObjectMapper objectMapper
    ) {
        this.paymentRepository = paymentRepository;
        this.commandRepository = commandRepository;
        this.bookingClient = bookingClient;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PaymentResponse initiate(AuthenticatedUser user, InitiatePaymentRequest request, String idempotencyKey) {
        PaymentResponse cached = loadCached(idempotencyKey, "INITIATE");
        if (cached != null) {
            return cached;
        }
        paymentRepository.findByBookingIdAndStatusIn(request.bookingId(), List.of(PaymentStatus.INITIATED, PaymentStatus.SUCCEEDED))
                .ifPresent(existing -> {
                    throw ApiException.conflict("Payment already exists for this booking");
                });
        Payment payment = new Payment();
        payment.setBookingId(request.bookingId());
        payment.setCustomerId(user.getId());
        payment.setAmountCents(request.amountCents());
        payment.setCurrency(StringUtils.hasText(request.currency()) ? request.currency() : "INR");
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setProvider("MOCK");
        paymentRepository.save(payment);
        PaymentResponse response = PaymentResponse.from(payment);
        cache(idempotencyKey, "INITIATE", payment.getId(), response);
        publish("payment.initiated", payment);
        return response;
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(AuthenticatedUser user, UUID paymentId) {
        Payment payment = require(paymentId);
        if (!user.isAdmin() && !user.getId().equals(payment.getCustomerId())) {
            throw ApiException.forbidden("Not allowed to view this payment");
        }
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse webhook(String secret, WebhookRequest request, String idempotencyKey) {
        if (!webhookSecret.equals(secret)) {
            throw ApiException.unauthorized("Invalid webhook secret");
        }
        PaymentResponse cached = loadCached(idempotencyKey, "WEBHOOK");
        if (cached != null) {
            return cached;
        }
        Payment payment = require(request.paymentId());
        String status = request.status().trim().toUpperCase();
        if ("SUCCEEDED".equals(status) || "SUCCESS".equals(status)) {
            if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
                PaymentResponse response = PaymentResponse.from(payment);
                cache(idempotencyKey, "WEBHOOK", payment.getId(), response);
                return response;
            }
            if (payment.getStatus() != PaymentStatus.INITIATED) {
                throw ApiException.conflict("Payment cannot succeed from " + payment.getStatus());
            }
            payment.setStatus(PaymentStatus.SUCCEEDED);
            if (StringUtils.hasText(request.providerRef())) {
                payment.setProviderRef(request.providerRef());
            }
            bookingClient.confirm(payment.getBookingId(), payment.getId(), idempotencyKey);
            publish("payment.succeeded", payment);
        } else if ("FAILED".equals(status) || "FAILURE".equals(status)) {
            if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
                throw ApiException.conflict("Succeeded payment cannot fail");
            }
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(StringUtils.hasText(request.failureReason()) ? request.failureReason() : "Payment failed");
            publish("payment.failed", payment);
        } else {
            throw ApiException.badRequest("Unsupported webhook status");
        }
        PaymentResponse response = PaymentResponse.from(payment);
        cache(idempotencyKey, "WEBHOOK", payment.getId(), response);
        return response;
    }

    @Transactional
    public PaymentResponse mockSucceed(UUID paymentId) {
        Payment payment = require(paymentId);
        return webhook(webhookSecret, new WebhookRequest(paymentId, "SUCCEEDED", payment.getProviderRef(), null), "mock-" + paymentId);
    }

    @Transactional
    public PaymentResponse refund(AuthenticatedUser user, UUID paymentId, String reason, String idempotencyKey) {
        PaymentResponse cached = loadCached(idempotencyKey, "REFUND");
        if (cached != null) {
            return cached;
        }
        Payment payment = require(paymentId);
        if (!user.isAdmin() && !user.getId().equals(payment.getCustomerId())) {
            throw ApiException.forbidden("Not allowed to refund this payment");
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            PaymentResponse response = PaymentResponse.from(payment);
            cache(idempotencyKey, "REFUND", paymentId, response);
            return response;
        }
        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw ApiException.conflict("Only succeeded payments can be refunded");
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setFailureReason(StringUtils.hasText(reason) ? reason : "Refunded");
        PaymentResponse response = PaymentResponse.from(payment);
        cache(idempotencyKey, "REFUND", paymentId, response);
        publish("payment.refunded", payment);
        return response;
    }

    private Payment require(UUID paymentId) {
        return paymentRepository.findById(paymentId).orElseThrow(() -> ApiException.notFound("Payment not found"));
    }

    private void publish(String type, Payment payment) {
        eventPublisher.publish(
                Topics.PAYMENT_EVENTS,
                payment.getId().toString(),
                DomainEvent.of(type, payment.getId().toString(), payment.getCustomerId(), write(PaymentResponse.from(payment)))
        );
    }

    private PaymentResponse loadCached(String idempotencyKey, String type) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return null;
        }
        return commandRepository.findById(idempotencyKey)
                .filter(cmd -> type.equals(cmd.getCommandType()))
                .map(cmd -> read(cmd.getResponseJson()))
                .orElse(null);
    }

    private void cache(String idempotencyKey, String type, UUID paymentId, PaymentResponse response) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return;
        }
        PaymentCommand command = new PaymentCommand();
        command.setIdempotencyKey(idempotencyKey);
        command.setCommandType(type);
        command.setPaymentId(paymentId);
        command.setResponseJson(write(response));
        commandRepository.save(command);
    }

    private String write(PaymentResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize payment response");
        }
    }

    private PaymentResponse read(String json) {
        try {
            return objectMapper.readValue(json, PaymentResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to deserialize cached payment response");
        }
    }
}
