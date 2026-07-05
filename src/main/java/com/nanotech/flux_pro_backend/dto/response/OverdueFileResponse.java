package com.nanotech.flux_pro_backend.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Ligne du widget « Top retards » (DSH-01/02/03) — même périmètre que le digest ALR-08. */
public record OverdueFileResponse(
        UUID fileId,
        String referenceNumber,
        String subject,
        String fileTypeCode,
        String organizationCode,
        String stepLabel,
        String responsibleUserName,
        Instant dueAt,
        int daysOverdue) {
}
