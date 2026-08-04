package com.nanotech.flux_pro_backend.service.assistant;

import com.nanotech.flux_pro_backend.common.AppException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssistantRateLimiterTest {

    @Test
    void allowsUpToLimitThenRejects() {
        AssistantRateLimiter limiter = new AssistantRateLimiter(3);
        UUID userId = UUID.randomUUID();

        assertDoesNotThrow(() -> limiter.checkOrThrow(userId));
        assertDoesNotThrow(() -> limiter.checkOrThrow(userId));
        assertDoesNotThrow(() -> limiter.checkOrThrow(userId));

        AppException ex = assertThrows(AppException.class, () -> limiter.checkOrThrow(userId));
        assertEquals("ASSISTANT_RATE_LIMIT", ex.getCode());
    }

    @Test
    void isolatesUsers() {
        AssistantRateLimiter limiter = new AssistantRateLimiter(1);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        assertDoesNotThrow(() -> limiter.checkOrThrow(a));
        assertDoesNotThrow(() -> limiter.checkOrThrow(b));
        assertThrows(AppException.class, () -> limiter.checkOrThrow(a));
    }
}
