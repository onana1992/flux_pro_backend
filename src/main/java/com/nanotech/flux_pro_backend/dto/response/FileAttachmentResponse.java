package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.AttachmentKind;

import java.time.Instant;
import java.util.UUID;

/** DTO pièce jointe — {@code kind} est la source de vérité ; {@code responseDocument} = kind CLOSURE (compat). */
public record FileAttachmentResponse(
        UUID id,
        String originalFilename,
        String contentType,
        long sizeBytes,
        AttachmentKind kind,
        boolean responseDocument,
        UUID passageId,
        String passageLabel,
        Integer passageStepOrder,
        boolean portalVisible,
        UUID uploadedById,
        String uploadedByName,
        Instant createdAt
) {
}
