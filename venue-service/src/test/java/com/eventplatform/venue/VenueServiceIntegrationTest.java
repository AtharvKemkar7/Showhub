package com.eventplatform.venue;

import com.eventplatform.common.security.Role;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
class VenueServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Test
    void adminCreatesVenueHallAndSeats() throws Exception {
        String admin = token(Role.ADMIN);
        String venueId = createVenue(admin, "Grand Arena", "Mumbai");
        MvcResult hallResult = mockMvc.perform(post("/api/v1/admin/venues/" + venueId + "/halls")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Hall A","capacity":6}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String hallId = objectMapper.readTree(hallResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/admin/venues/" + venueId + "/halls/" + hallId + "/layout")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rows":["A","B"],"seatsPerRow":3,"defaultType":"REGULAR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(6)));
    }

    @Test
    void layoutCannotExceedCapacity() throws Exception {
        String admin = token(Role.ADMIN);
        String venueId = createVenue(admin, "Small Hall Venue", "Pune");
        String hallId = createHall(admin, venueId, "Studio", 2);
        mockMvc.perform(post("/api/v1/admin/venues/" + venueId + "/halls/" + hallId + "/layout")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rows":["A"],"seatsPerRow":4,"defaultType":"VIP"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void duplicateSeatRejected() throws Exception {
        String admin = token(Role.ADMIN);
        String venueId = createVenue(admin, "Dup Seat Venue", "Delhi");
        String hallId = createHall(admin, venueId, "Main", 10);
        mockMvc.perform(post("/api/v1/admin/venues/" + venueId + "/halls/" + hallId + "/seats")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rowLabel":"A","seatNumber":1,"seatType":"PREMIUM"}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/admin/venues/" + venueId + "/halls/" + hallId + "/seats")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rowLabel":"A","seatNumber":1,"seatType":"VIP"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void customerCannotManageVenues() throws Exception {
        mockMvc.perform(post("/api/v1/admin/venues")
                        .header("Authorization", "Bearer " + token(Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"X","address":"1 St","city":"Goa","state":"GA"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void organizerCanViewAvailableVenuesOnly() throws Exception {
        String admin = token(Role.ADMIN);
        String venueId = createVenue(admin, "Open Air", "Jaipur");
        mockMvc.perform(patch("/api/v1/admin/venues/" + venueId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/venues/" + venueId)
                        .header("Authorization", "Bearer " + token(Role.ORGANIZER)))
                .andExpect(status().isNotFound());
    }

    private String createVenue(String token, String name, String city) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/venues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","address":"1 Main Road","city":"%s","state":"MH"}
                                """.formatted(name, city)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createHall(String token, String venueId, String name, int capacity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/venues/" + venueId + "/halls")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","capacity":%d}
                                """.formatted(name, capacity)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asText();
    }

    private String token(Role role) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", role.name().toLowerCase() + "@example.com")
                .claim("role", role.name())
                .claim("typ", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }
}
