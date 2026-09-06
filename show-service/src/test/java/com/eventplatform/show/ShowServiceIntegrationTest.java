package com.eventplatform.show;

import com.eventplatform.common.security.Role;
import com.eventplatform.common.test.TestTokens;
import com.eventplatform.show.support.StubCatalogClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(StubCatalogClient.class)
@Transactional
class ShowServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void organizerCreatesShowForOwnEvent() throws Exception {
        String token = TestTokens.token(StubCatalogClient.ORGANIZER, Role.ORGANIZER);
        mockMvc.perform(post("/api/v1/organizer/shows")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(showJson(StubCatalogClient.PUBLISHED_EVENT, Instant.now().plus(2, ChronoUnit.DAYS))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizerId").value(StubCatalogClient.ORGANIZER.toString()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void organizerCannotManageAnotherOrganizersEvent() throws Exception {
        String token = TestTokens.token(StubCatalogClient.ORGANIZER, Role.ORGANIZER);
        mockMvc.perform(post("/api/v1/organizer/shows")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(showJson(StubCatalogClient.OTHER_EVENT, Instant.now().plus(2, ChronoUnit.DAYS))))
                .andExpect(status().isForbidden());
    }

    @Test
    void overlappingShowsAreRejected() throws Exception {
        String token = TestTokens.token(StubCatalogClient.ORGANIZER, Role.ORGANIZER);
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS);
        mockMvc.perform(post("/api/v1/organizer/shows")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(showJson(StubCatalogClient.PUBLISHED_EVENT, start)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/organizer/shows")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(showJson(StubCatalogClient.PUBLISHED_EVENT, start.plus(30, ChronoUnit.MINUTES))))
                .andExpect(status().isConflict());
    }

    @Test
    void customerCannotCreateShows() throws Exception {
        mockMvc.perform(post("/api/v1/organizer/shows")
                        .header("Authorization", "Bearer " + TestTokens.token(Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(showJson(StubCatalogClient.PUBLISHED_EVENT, Instant.now().plus(2, ChronoUnit.DAYS))))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicCanViewScheduledShow() throws Exception {
        String token = TestTokens.token(StubCatalogClient.ORGANIZER, Role.ORGANIZER);
        MvcResult result = mockMvc.perform(post("/api/v1/organizer/shows")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(showJson(StubCatalogClient.PUBLISHED_EVENT, Instant.now().plus(4, ChronoUnit.DAYS))))
                .andExpect(status().isCreated())
                .andReturn();
        String showId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(get("/api/v1/shows/" + showId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(StubCatalogClient.PUBLISHED_EVENT.toString()));
    }

    private String showJson(java.util.UUID eventId, Instant start) {
        Instant end = start.plus(2, ChronoUnit.HOURS);
        return """
                {
                  "eventId": "%s",
                  "venueId": "%s",
                  "hallId": "%s",
                  "language": "en",
                  "startAt": "%s",
                  "endAt": "%s",
                  "prices": [
                    {"seatType":"REGULAR","amountCents":50000},
                    {"seatType":"PREMIUM","amountCents":80000}
                  ]
                }
                """.formatted(eventId, StubCatalogClient.VENUE, StubCatalogClient.HALL, start, end);
    }
}
