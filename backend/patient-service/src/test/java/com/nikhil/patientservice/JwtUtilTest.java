package com.nikhil.patientservice;

import com.nikhil.patientservice.config.JwtUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private final JwtUtil jwt = new JwtUtil("unit-test-secret-that-is-at-least-32-bytes", 60_000);

    @Test
    void roundTrip() {
        String token = jwt.generateToken("nikhil", "ADMIN");
        assertThat(jwt.validateToken(token)).isTrue();
        assertThat(jwt.extractUsername(token)).isEqualTo("nikhil");
        assertThat(jwt.parse(token).orElseThrow().get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void tamperedOrForeignTokensAreRejected() {
        String token = jwt.generateToken("nikhil", "ADMIN");
        JwtUtil other = new JwtUtil("a-completely-different-secret-of-32-bytes!", 60_000);
        assertThat(other.validateToken(token)).isFalse();
        assertThat(jwt.validateToken(token + "x")).isFalse();
        assertThat(jwt.validateToken("garbage")).isFalse();
        assertThat(jwt.extractUsername("garbage")).isNull();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtUtil shortLived = new JwtUtil("unit-test-secret-that-is-at-least-32-bytes", -1_000);
        assertThat(shortLived.validateToken(shortLived.generateToken("nikhil", "ADMIN"))).isFalse();
    }

    @Test
    void shortSecretFailsFast() {
        assertThatThrownBy(() -> new JwtUtil("too-short", 1000)).isInstanceOf(IllegalStateException.class);
    }
}
