package com.procureai.ai.chat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.procureai.common.exception.RateLimitExceededException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class InMemoryRateLimiterTest {

    @Test
    void rejectsCallsOverLimit() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(
                2,
                Clock.fixed(Instant.parse("2026-07-14T00:00:00Z"), ZoneOffset.UTC)
        );

        limiter.assertAllowed("user");
        limiter.assertAllowed("user");

        assertThatThrownBy(() -> limiter.assertAllowed("user"))
                .isInstanceOf(RateLimitExceededException.class);
    }
}
