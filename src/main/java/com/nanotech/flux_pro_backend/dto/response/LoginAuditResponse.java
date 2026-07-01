package com.nanotech.flux_pro_backend.dto.response;

import java.time.Instant;
import java.util.UUID;

public record LoginAuditResponse(
        UUID id,
        String email,
        boolean success,
        String ipAddress,
        String userAgent,
        String failureReason,
        Instant createdAt
) {
}
