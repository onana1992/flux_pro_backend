package com.nanotech.flux_pro_backend.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record LoginAuditResponse(
        UUID id,
        String email,
        boolean succes,
        String ipAddress,
        String userAgent,
        String motifEchec,
        Instant createdAt
) {
}
