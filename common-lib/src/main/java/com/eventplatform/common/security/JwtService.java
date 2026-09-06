package com.eventplatform.common.security;

import com.eventplatform.common.exception.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class JwtService {

    private static final String ACCESS_TYPE = "access";
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        if (!StringUtils.hasText(properties.getSecret()) || properties.getSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be configured and at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public AuthenticatedUser parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!ACCESS_TYPE.equals(claims.get("typ", String.class))) {
                throw ApiException.unauthorized("Invalid access token");
            }
            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get("email", String.class);
            Role role = Role.valueOf(claims.get("role", String.class));
            return new AuthenticatedUser(userId, email, role);
        } catch (JwtException | IllegalArgumentException ex) {
            throw ApiException.unauthorized("Invalid or expired access token");
        }
    }
}
