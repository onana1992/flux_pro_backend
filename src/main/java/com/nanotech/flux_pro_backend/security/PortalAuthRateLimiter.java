package com.nanotech.flux_pro_backend.security;

import com.nanotech.flux_pro_backend.common.AppException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting in-memory pour login / OTP / activate portail (P3).
 */
@Component
public class PortalAuthRateLimiter {

    private static final int LOGIN_MAX = 10;
    private static final int OTP_MAX = 5;
    private static final int ACTIVATE_MAX = 10;
    private static final long WINDOW_SECONDS = 900;

    private final Map<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    public void checkLogin(String key) {
        check("login:" + normalize(key), LOGIN_MAX, "PORTAL_RATE_LIMIT_LOGIN", "Too many login attempts");
    }

    public void checkOtp(String key) {
        check("otp:" + normalize(key), OTP_MAX, "PORTAL_RATE_LIMIT_OTP", "Too many OTP requests");
    }

    public void checkActivate(String key) {
        check("activate:" + normalize(key), ACTIVATE_MAX, "PORTAL_RATE_LIMIT_ACTIVATE", "Too many activate attempts");
    }

    private void check(String bucketKey, int max, String code, String message) {
        long now = Instant.now().getEpochSecond();
        long cutoff = now - WINDOW_SECONDS;
        Deque<Long> q = buckets.computeIfAbsent(bucketKey, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst() < cutoff) {
                q.removeFirst();
            }
            if (q.size() >= max) {
                throw AppException.badRequest(code, message);
            }
            q.addLast(now);
        }
    }

    private String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }
}
