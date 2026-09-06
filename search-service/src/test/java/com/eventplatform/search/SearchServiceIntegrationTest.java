package com.eventplatform.search;

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
class SearchServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void indexesAndSearchesPublishedEvents() throws Exception {
        UUID eventId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/internal/index")
                        .header("X-Internal-Token", "dev-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId":"%s",
                                  "title":"Jazz Night",
                                  "description":"Live jazz",
                                  "city":"Mumbai",
                                  "categoryName":"Music",
                                  "status":"PUBLISHED",
                                  "startAt":"2026-10-01T18:00:00Z"
                                }
                                """.formatted(eventId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Jazz Night"));

        mockMvc.perform(get("/api/v1/search/events").param("q", "Jazz").param("city", "Mumbai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].eventId").value(eventId.toString()));
    }

    @Test
    void unpublishedEventsAreNotReturned() throws Exception {
        mockMvc.perform(post("/api/v1/internal/index")
                        .header("X-Internal-Token", "dev-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId":"%s",
                                  "title":"Draft Show",
                                  "city":"Delhi",
                                  "status":"DRAFT"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/search/events").param("q", "Draft"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }
}
