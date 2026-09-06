package com.eventplatform.event;

import com.eventplatform.event.domain.Role;
import com.eventplatform.event.repository.EventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventServiceIntegrationTest {

    private static final UUID MUSIC_CATEGORY = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private UUID organizerId;
    private UUID otherOrganizerId;
    private UUID customerId;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        organizerId = UUID.randomUUID();
        otherOrganizerId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        adminId = UUID.randomUUID();
    }

    @Test
    void publicUsersSeeOnlyPublishedEvents() throws Exception {
        String organizerToken = token(organizerId, Role.ORGANIZER);
        String adminToken = token(adminId, Role.ADMIN);
        String eventId = createAndSubmit(organizerToken, "Jazz Night", "Paris");

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get("/api/v1/events/" + eventId))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/admin/events/" + eventId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Jazz Night"));

        mockMvc.perform(get("/api/v1/events").param("city", "Paris"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void customerCannotAccessOrganizerOrAdminOperations() throws Exception {
        String customerToken = token(customerId, Role.CUSTOMER);
        mockMvc.perform(post("/api/v1/organizer/events")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("Nope", "London")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/events")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void organizerCannotAccessAdminOperations() throws Exception {
        String organizerToken = token(organizerId, Role.ORGANIZER);
        mockMvc.perform(get("/api/v1/admin/events")
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void organizerCannotModifyAnotherOrganizersEvent() throws Exception {
        String ownerToken = token(organizerId, Role.ORGANIZER);
        String otherToken = token(otherOrganizerId, Role.ORGANIZER);
        String eventId = createEvent(ownerToken, "Owned Show", "Berlin");

        mockMvc.perform(patch("/api/v1/organizer/events/" + eventId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Hijacked"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/organizer/events/" + eventId + "/submit")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void organizerIdFromClientIsIgnored() throws Exception {
        String organizerToken = token(organizerId, Role.ORGANIZER);
        MvcResult result = mockMvc.perform(post("/api/v1/organizer/events")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizerId": "%s",
                                  "categoryId": "%s",
                                  "title": "Secure Event",
                                  "description": "Ownership comes from JWT",
                                  "venueName": "Hall",
                                  "venueAddress": "1 Main St",
                                  "city": "Rome",
                                  "startAt": "%s",
                                  "endAt": "%s"
                                }
                                """.formatted(
                                otherOrganizerId,
                                MUSIC_CATEGORY,
                                Instant.now().plus(2, ChronoUnit.DAYS),
                                Instant.now().plus(3, ChronoUnit.DAYS))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizerId").value(organizerId.toString()))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(body.get("organizerId").asText()).isEqualTo(organizerId.toString());
    }

    @Test
    void adminCanRejectAndOrganizerCanResubmit() throws Exception {
        String organizerToken = token(organizerId, Role.ORGANIZER);
        String adminToken = token(adminId, Role.ADMIN);
        String eventId = createAndSubmit(organizerToken, "Need Review", "Madrid");

        mockMvc.perform(post("/api/v1/admin/events/" + eventId + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Missing safety details"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(post("/api/v1/organizer/events/" + eventId + "/submit")
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
    }

    @Test
    void adminCanCancelPublishedEvent() throws Exception {
        String organizerToken = token(organizerId, Role.ORGANIZER);
        String adminToken = token(adminId, Role.ADMIN);
        String eventId = createAndSubmit(organizerToken, "Cancel Me", "Oslo");
        mockMvc.perform(post("/api/v1/admin/events/" + eventId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/events/" + eventId + "/cancel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/v1/events/" + eventId))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedOrganizerCreateReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/organizer/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("No Auth", "Lisbon")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidEventPayloadReturns400() throws Exception {
        String organizerToken = token(organizerId, Role.ORGANIZER);
        mockMvc.perform(post("/api/v1/organizer/events")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void categoriesArePublic() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    private String createAndSubmit(String organizerToken, String title, String city) throws Exception {
        String eventId = createEvent(organizerToken, title, city);
        mockMvc.perform(post("/api/v1/organizer/events/" + eventId + "/submit")
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
        return eventId;
    }

    private String createEvent(String organizerToken, String title, String city) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/organizer/events")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(title, city)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String eventJson(String title, String city) {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant end = start.plus(3, ChronoUnit.HOURS);
        return """
                {
                  "categoryId": "%s",
                  "title": "%s",
                  "description": "A great live event",
                  "venueName": "Main Hall",
                  "venueAddress": "10 Market Street",
                  "city": "%s",
                  "startAt": "%s",
                  "endAt": "%s"
                }
                """.formatted(MUSIC_CATEGORY, title, city, start, end);
    }

    private String token(UUID userId, Role role) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", userId + "@example.com")
                .claim("role", role.name())
                .claim("typ", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }
}
