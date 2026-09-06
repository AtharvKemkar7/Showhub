package com.eventplatform.identity;

import com.eventplatform.identity.domain.Role;
import com.eventplatform.identity.domain.UserStatus;
import com.eventplatform.identity.repository.RefreshTokenRepository;
import com.eventplatform.identity.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IdentityServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanNonAdminUsers() {
        refreshTokenRepository.deleteAll();
        userRepository.findAll().stream()
                .filter(user -> user.getRole() != Role.ADMIN)
                .forEach(userRepository::delete);
    }

    @Test
    void registerLoginAndReadProfile() throws Exception {
        JsonNode tokens = register("customer1@example.com", "Password123", "CUSTOMER");
        String access = tokens.get("accessToken").asText();

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("customer1@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void publicRegistrationCannotCreateAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "hacker@example.com",
                                  "password": "Password123",
                                  "firstName": "Bad",
                                  "lastName": "Actor",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        register("dup@example.com", "Password123", "CUSTOMER");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "dup@example.com",
                                  "password": "Password123",
                                  "firstName": "Dup",
                                  "lastName": "User",
                                  "role": "CUSTOMER"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidLoginReturnsUnauthorized() throws Exception {
        register("login@example.com", "Password123", "CUSTOMER");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"login@example.com","password":"WrongPass1"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", not(containsString("WrongPass"))));
    }

    @Test
    void customerCannotAccessAdminEndpoints() throws Exception {
        JsonNode tokens = register("cust@example.com", "Password123", "CUSTOMER");
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + tokens.get("accessToken").asText()))
                .andExpect(status().isForbidden());
    }

    @Test
    void organizerCannotAccessAdminEndpoints() throws Exception {
        JsonNode tokens = register("org@example.com", "Password123", "ORGANIZER");
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + tokens.get("accessToken").asText()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanChangeRoleAndStatus() throws Exception {
        JsonNode customer = register("promote@example.com", "Password123", "CUSTOMER");
        String userId = customer.get("user").get("id").asText();
        String adminToken = login("admin@eventplatform.local", "AdminPass123!");

        mockMvc.perform(patch("/api/v1/admin/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"ORGANIZER","status":"SUSPENDED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ORGANIZER"))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void suspendedUserCannotLogin() throws Exception {
        JsonNode customer = register("suspend@example.com", "Password123", "CUSTOMER");
        String userId = customer.get("user").get("id").asText();
        String adminToken = login("admin@eventplatform.local", "AdminPass123!");

        mockMvc.perform(patch("/api/v1/admin/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"SUSPENDED"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"suspend@example.com","password":"Password123"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshRotatesToken() throws Exception {
        JsonNode tokens = register("refresh@example.com", "Password123", "CUSTOMER");
        String refresh = tokens.get("refreshToken").asText();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());

        JsonNode rotated = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(rotated.get("refreshToken").asText()).isNotEqualTo(refresh);
    }

    @Test
    void unauthenticatedProfileReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validationFailureReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"short","firstName":"","lastName":"","role":"CUSTOMER"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void userCannotChangeOwnRoleViaProfile() throws Exception {
        JsonNode tokens = register("norole@example.com", "Password123", "CUSTOMER");
        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tokens.get("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"New","role":"ADMIN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.firstName").value("New"));
    }

    private JsonNode register(String email, String password, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "firstName": "Test",
                                  "lastName": "User",
                                  "role": "%s"
                                }
                                """.formatted(email, password, role)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
