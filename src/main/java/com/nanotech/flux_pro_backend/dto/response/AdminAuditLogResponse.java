package com.nanotech.flux_pro_backend.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditLogResponse(
        UUID id,
        String actorEmail,
        String resourceType,
        String action,
        String resourceId,
        String resourceLabel,
        boolean success,
        String errorMessage,
        String ipAddress,
        String userAgent,
        Instant createdAt
) {
}
