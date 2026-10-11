package com.nikhil.notification;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long!!";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private NotificationHub hub;

    @Test
    void recentKeepsNewestFirstAndCapsHistory() {
        NotificationHub fresh = new NotificationHub();
        for (int i = 0; i < NotificationHub.HISTORY_SIZE + 5; i++) {
            fresh.broadcast("{\"alertId\":" + i + "}");
        }
        assertThat(fresh.recent()).hasSize(NotificationHub.HISTORY_SIZE);
        assertThat(fresh.recent().get(0)).isEqualTo("{\"alertId\":" + (NotificationHub.HISTORY_SIZE + 4) + "}");
    }

    @Test
    void recentEndpointReturnsJsonArray() throws Exception {
        hub.broadcast("{\"alertId\":42,\"severity\":\"HIGH\"}");
        mvc.perform(get("/api/notifications/recent").header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].alertId").value(42));
        mvc.perform(get("/api/notifications/recent")).andExpect(status().isUnauthorized());
    }

    @Test
    void streamRejectsMissingOrBadToken() throws Exception {
        mvc.perform(get("/api/notifications/stream")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notifications/stream").param("token", "bad")).andExpect(status().isUnauthorized());
    }

    @Test
    void streamOpensWithValidToken() throws Exception {
        mvc.perform(get("/api/notifications/stream").param("token", token()))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk());
        assertThat(hub.connectionCount()).isGreaterThanOrEqualTo(1);
    }

    private static String token() {
        return Jwts.builder().subject("clinician").claim("role", "CLINICIAN")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
}
