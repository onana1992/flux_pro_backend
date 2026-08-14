package com.nanotech.flux_pro_backend.service.assistant;

import com.nanotech.flux_pro_backend.common.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit simple en mémoire : N messages utilisateur / heure.
 */
@Component
public class AssistantRateLimiter {

    private final int maxMessagesPerHour;
    private final Map<UUID, Deque<Long>> hitsByUser = new ConcurrentHashMap<>();

    public AssistantRateLimiter(
            @Value("${fluxpro.assistant.rate-limit-per-hour:30}") int maxMessagesPerHour) {
        this.maxMessagesPerHour = maxMessagesPerHour <= 0 ? 30 : maxMessagesPerHour;
    }

    public void checkOrThrow(UUID userId) {
        long now = System.currentTimeMillis();
        long windowStart = now - 3_600_000L;
        Deque<Long> hits = hitsByUser.computeIfAbsent(userId, id -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && hits.peekFirst() < windowStart) {
                hits.pollFirst();
            }
            if (hits.size() >= maxMessagesPerHour) {
                throw AppException.badRequest(
                        "ASSISTANT_RATE_LIMIT",
                        "Limite de messages atteinte ({0}/heure). Réessayez plus tard.",
                        maxMessagesPerHour);
            }
            hits.addLast(now);
        }
    }
}
