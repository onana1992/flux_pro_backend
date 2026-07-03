package com.nanotech.flux_pro_backend.dto.response;

import java.time.Instant;
import java.util.UUID;

public record FileAttachmentResponse(
        UUID id,
        String originalFilename,
        String contentType,
        long sizeBytes,
        boolean responseDocument,
        UUID uploadedById,
        String uploadedByName,
        Instant createdAt
) {
}
