package com.eventplatform.booking;

import com.eventplatform.booking.support.StubClients;
import com.eventplatform.common.security.Role;
import com.eventplatform.common.test.TestTokens;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(StubClients.class)
@Transactional
class BookingServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StubClients.InMemoryInventoryClient inventoryClient;

    @BeforeEach
    void resetInventory() {
        inventoryClient.reset();
    }

    @Test
    void customerCreatesBooking() throws Exception {
        UUID customer = UUID.randomUUID();
        String token = TestTokens.token(customer, Role.CUSTOMER);
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.customerId").value(customer.toString()))
                .andExpect(jsonPath("$.totalCents").value(50000));
    }

    @Test
    void createIsIdempotent() throws Exception {
        String token = TestTokens.token(UUID.randomUUID(), Role.CUSTOMER);
        String key = "book-" + UUID.randomUUID();
        MvcResult first = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isCreated())
                .andReturn();
        MvcResult second = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isCreated())
                .andReturn();
        String id1 = objectMapper.readTree(first.getResponse().getContentAsString()).get("id").asText();
        String id2 = objectMapper.readTree(second.getResponse().getContentAsString()).get("id").asText();
        org.assertj.core.api.Assertions.assertThat(id1).isEqualTo(id2);
    }

    @Test
    void customerCannotViewAnotherBooking() throws Exception {
        String owner = TestTokens.token(UUID.randomUUID(), Role.CUSTOMER);
        MvcResult created = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isCreated())
                .andReturn();
        String bookingId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(get("/api/v1/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + TestTokens.token(UUID.randomUUID(), Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalConfirmMarksConfirmed() throws Exception {
        String token = TestTokens.token(UUID.randomUUID(), Role.CUSTOMER);
        MvcResult created = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isCreated())
                .andReturn();
        String bookingId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        UUID paymentId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/internal/bookings/" + bookingId + "/confirm")
                        .header("X-Internal-Token", "dev-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentId":"%s"}
                                """.formatted(paymentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void organizerCannotCreateBooking() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + TestTokens.token(Role.ORGANIZER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isForbidden());
    }

    private String createBody() {
        return """
                {"showId":"%s","seatIds":["%s"]}
                """.formatted(StubClients.SHOW_ID, StubClients.SEAT_A);
    }
}
