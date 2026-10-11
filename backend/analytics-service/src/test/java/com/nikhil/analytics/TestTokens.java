package com.nikhil.analytics;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Mints tokens exactly like patient-service does, with the test secret. */
final class TestTokens {

    static final String SECRET = "test-secret-that-is-at-least-32-bytes-long!!";

    private TestTokens() {
    }

    static String token(String user, String role) {
        return Jwts.builder()
                .subject(user)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
