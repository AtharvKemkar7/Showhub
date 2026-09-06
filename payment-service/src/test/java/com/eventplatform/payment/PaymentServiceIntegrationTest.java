package com.eventplatform.payment;

import com.eventplatform.common.security.Role;
import com.eventplatform.common.test.TestTokens;
import com.eventplatform.payment.support.StubBookingClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(StubBookingClient.class)
@Transactional
class PaymentServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void reset() {
        StubBookingClient.CONFIRMS.set(0);
        StubBookingClient.CONFIRMED.clear();
    }

    @Test
    void initiateAndWebhookSucceeds() throws Exception {
        UUID customer = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        String token = TestTokens.token(customer, Role.CUSTOMER);
        MvcResult created = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookingId":"%s","amountCents":50000,"currency":"INR"}
                                """.formatted(bookingId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("INITIATED"))
                .andReturn();
        String paymentId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Webhook-Secret", "test-webhook-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentId":"%s","status":"SUCCEEDED"}
                                """.formatted(paymentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
        assertThat(StubBookingClient.CONFIRMED).containsKey(bookingId);
    }

    @Test
    void webhookIsIdempotent() throws Exception {
        String token = TestTokens.token(UUID.randomUUID(), Role.CUSTOMER);
        UUID bookingId = UUID.randomUUID();
        MvcResult created = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookingId":"%s","amountCents":1000}
                                """.formatted(bookingId)))
                .andExpect(status().isCreated())
                .andReturn();
        String paymentId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        String key = "wh-" + UUID.randomUUID();
        String body = """
                {"paymentId":"%s","status":"SUCCEEDED"}
                """.formatted(paymentId);
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Webhook-Secret", "test-webhook-secret")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Webhook-Secret", "test-webhook-secret")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
        assertThat(StubBookingClient.CONFIRMS.get()).isEqualTo(1);
    }

    @Test
    void refundSucceededPayment() throws Exception {
        UUID customer = UUID.randomUUID();
        String token = TestTokens.token(customer, Role.CUSTOMER);
        MvcResult created = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookingId":"%s","amountCents":2500}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn();
        String paymentId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/mock-checkout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"customer request"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    void invalidWebhookSecretRejected() throws Exception {
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Webhook-Secret", "wrong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentId":"%s","status":"SUCCEEDED"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }
}
