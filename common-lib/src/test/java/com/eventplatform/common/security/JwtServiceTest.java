package com.eventplatform.common.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void parsesCompatibleIdentityAccessToken() {
        String secret = "test-jwt-secret-key-that-is-at-least-32-chars-long";
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        JwtService jwtService = new JwtService(properties);
        UUID userId = UUID.randomUUID();
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("email", "user@example.com")
                .claim("role", "ORGANIZER")
                .claim("typ", "access")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
        AuthenticatedUser user = jwtService.parseAccessToken(token);
        assertThat(user.getId()).isEqualTo(userId);
        assertThat(user.getRole()).isEqualTo(Role.ORGANIZER);
    }
}
