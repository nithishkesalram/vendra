package com.procureai.ai.chat;

import com.procureai.common.exception.RateLimitExceededException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRateLimiter {

    private static final long WINDOW_MILLIS = 60_000L;

    private final int limitPerMinute;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public InMemoryRateLimiter(
            @Value("${procureai.ai.chat-rate-limit-per-minute:20}") int limitPerMinute
    ) {
        this(limitPerMinute, Clock.systemUTC());
    }

    InMemoryRateLimiter(int limitPerMinute, Clock clock) {
        this.limitPerMinute = limitPerMinute;
        this.clock = clock;
    }

    public synchronized void assertAllowed(String key) {
        long now = clock.millis();
        Window window = windows.computeIfAbsent(key, ignored -> new Window(now, 0));
        if (now - window.windowStartedAt >= WINDOW_MILLIS) {
            window.windowStartedAt = now;
            window.count = 0;
        }
        if (window.count >= limitPerMinute) {
            long retryAfterMillis = WINDOW_MILLIS - (now - window.windowStartedAt);
            throw new RateLimitExceededException(Math.max(1, (retryAfterMillis + 999) / 1000));
        }
        window.count++;
    }

    private static class Window {
        private long windowStartedAt;
        private int count;

        private Window(long windowStartedAt, int count) {
            this.windowStartedAt = windowStartedAt;
            this.count = count;
        }
    }
}
