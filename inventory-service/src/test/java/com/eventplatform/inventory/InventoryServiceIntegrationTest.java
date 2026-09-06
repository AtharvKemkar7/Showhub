package com.eventplatform.inventory;

import com.eventplatform.common.security.Role;
import com.eventplatform.common.test.TestTokens;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private UUID showId;
    private UUID seatA;
    private UUID seatB;

    @BeforeEach
    void setUp() throws Exception {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        showId = UUID.randomUUID();
        seatA = UUID.randomUUID();
        seatB = UUID.randomUUID();
        String admin = TestTokens.token(Role.ADMIN);
        mockMvc.perform(post("/api/v1/inventory/shows")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "showId": "%s",
                                  "seats": [
                                    {"seatId":"%s","rowLabel":"A","seatNumber":1,"seatType":"REGULAR"},
                                    {"seatId":"%s","rowLabel":"A","seatNumber":2,"seatType":"PREMIUM"}
                                  ]
                                }
                                """.formatted(showId, seatA, seatB)))
                .andExpect(status().isCreated());
    }

    @Test
    void publicSeatMap() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/shows/" + showId + "/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats", hasSize(2)));
    }

    @Test
    void lockIsIdempotent() throws Exception {
        UUID customer = UUID.randomUUID();
        String token = TestTokens.token(customer, Role.CUSTOMER);
        String body = lockBody();
        String idempotencyKey = "lock-" + UUID.randomUUID();
        MvcResult first = mockMvc.perform(post("/api/v1/inventory/locks")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult second = mockMvc.perform(post("/api/v1/inventory/locks")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        String lock1 = objectMapper.readTree(first.getResponse().getContentAsString()).get("lockId").asText();
        String lock2 = objectMapper.readTree(second.getResponse().getContentAsString()).get("lockId").asText();
        assertThat(lock1).isEqualTo(lock2);
    }

    @Test
    void secondCustomerCannotDoubleBook() throws Exception {
        String first = TestTokens.token(UUID.randomUUID(), Role.CUSTOMER);
        String second = TestTokens.token(UUID.randomUUID(), Role.CUSTOMER);
        mockMvc.perform(post("/api/v1/inventory/locks")
                        .header("Authorization", "Bearer " + first)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lockBody()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/inventory/locks")
                        .header("Authorization", "Bearer " + second)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lockBody()))
                .andExpect(status().isConflict());
    }

    @Test
    void concurrentLocksAllowOnlyOneWinner() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(8);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        for (int i = 0; i < 8; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    String token = TestTokens.token(UUID.randomUUID(), Role.CUSTOMER);
                    int status = mockMvc.perform(post("/api/v1/inventory/locks")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(lockBody()))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                    if (status == 200) {
                        success.incrementAndGet();
                    } else if (status == 409) {
                        conflict.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await();
        pool.shutdown();
        assertThat(success.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(7);
    }

    @Test
    void confirmBooksSeats() throws Exception {
        UUID customer = UUID.randomUUID();
        String token = TestTokens.token(customer, Role.CUSTOMER);
        MvcResult lock = mockMvc.perform(post("/api/v1/inventory/locks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lockBody()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(lock.getResponse().getContentAsString());
        UUID bookingId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/inventory/locks/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lockId":"%s","bookingId":"%s"}
                                """.formatted(body.get("lockId").asText(), bookingId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BOOKED"));
        mockMvc.perform(post("/api/v1/internal/bookings/unbook")
                        .header("X-Internal-Token", "dev-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookingId":"%s"}
                                """.formatted(bookingId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    private String lockBody() {
        return """
                {"showId":"%s","seatIds":["%s"]}
                """.formatted(showId, seatA);
    }
}
