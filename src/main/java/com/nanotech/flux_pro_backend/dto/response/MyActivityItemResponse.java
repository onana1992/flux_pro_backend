package com.nanotech.flux_pro_backend.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Un maillon dont l'utilisateur courant est responsable (widget « Mon activité », DSH-01). */
public record MyActivityItemResponse(
        UUID passageId,
        UUID fileId,
        String fileReferenceNumber,
        String fileSubject,
        String stepLabel,
        Instant receivedAt,
        Instant dueAt,
        boolean overdue) {
}
