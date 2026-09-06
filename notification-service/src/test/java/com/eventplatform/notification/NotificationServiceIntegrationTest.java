package com.eventplatform.notification;

import com.eventplatform.common.security.Role;
import com.eventplatform.common.test.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dispatchStoresAndListsNotifications() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/internal/notifications")
                        .header("X-Internal-Token", "dev-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","eventType":"booking.confirmed","aggregateId":"b-1"}
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + TestTokens.token(userId, Role.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void consumeDomainEvent() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/internal/events")
                        .header("X-Internal-Token", "dev-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"booking.created",
                                  "aggregateId":"b-2",
                                  "actorId":"%s",
                                  "occurredAt":"2026-01-01T00:00:00Z",
                                  "payloadJson":"{}"
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SENT"));
    }
}
