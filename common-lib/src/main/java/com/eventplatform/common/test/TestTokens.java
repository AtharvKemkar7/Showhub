package com.eventplatform.common.test;

import com.eventplatform.common.security.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

public final class TestTokens {

    public static final String SECRET = "test-jwt-secret-key-that-is-at-least-32-chars-long";

    private TestTokens() {
    }

    public static String token(UUID userId, Role role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
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

    public static String token(Role role) {
        return token(UUID.randomUUID(), role);
    }
}
