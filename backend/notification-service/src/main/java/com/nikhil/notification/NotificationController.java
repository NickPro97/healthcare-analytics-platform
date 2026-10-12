package com.nikhil.notification;

import com.nikhil.notification.security.JwtVerifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationHub hub;
    private final JwtVerifier jwtVerifier;

    public NotificationController(NotificationHub hub, JwtVerifier jwtVerifier) {
        this.hub = hub;
        this.jwtVerifier = jwtVerifier;
    }

    /** text/event-stream of alerts. Token in the query string because EventSource cannot set headers. */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam(required = false) String token) {
        if (token == null || jwtVerifier.parse(token).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "valid token required");
        }
        return hub.subscribe();
    }

    /** Last 100 alerts, newest first, as a JSON array. */
    @GetMapping(path = "/recent", produces = MediaType.APPLICATION_JSON_VALUE)
    public String recent() {
        return "[" + String.join(",", hub.recent()) + "]";
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("connections", hub.connectionCount(), "buffered", hub.recent().size());
    }
}
