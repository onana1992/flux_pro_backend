package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.FilePriority;
import com.nanotech.flux_pro_backend.enumeration.FileStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FileSummaryResponse(
        UUID id,
        String referenceNumber,
        String fileTypeCode,
        String subject,
        FilePriority priority,
        FileStatus status,
        LocalDate receivedAt,
        String organizationCode,
        String organizationName,
        String chainTemplateCode,
        Instant createdAt
) {
}
