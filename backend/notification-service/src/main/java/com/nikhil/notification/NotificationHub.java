package com.nikhil.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Keeps the open browser connections and a short history of recent alerts.
 * Alerts are passed through as the JSON produced by analytics-service.
 */
@Component
public class NotificationHub {

    private static final Logger log = LoggerFactory.getLogger(NotificationHub.class);
    static final int HISTORY_SIZE = 100;
    static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final Deque<String> recent = new ArrayDeque<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("{\"status\":\"connected\"}", MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    public void broadcast(String alertJson) {
        synchronized (recent) {
            recent.addFirst(alertJson);
            while (recent.size() > HISTORY_SIZE) {
                recent.removeLast();
            }
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("alert").data(alertJson, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
                log.debug("Dropped a closed SSE connection");
            }
        }
    }

    /** Newest first. */
    public List<String> recent() {
        synchronized (recent) {
            return new ArrayList<>(recent);
        }
    }

    public int connectionCount() {
        return emitters.size();
    }

    /** Comment line every 20s so proxies (nginx, ALB) do not close idle streams. */
    @Scheduled(fixedRate = 20_000)
    public void heartbeat() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("keep-alive"));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
            }
        }
    }
}
