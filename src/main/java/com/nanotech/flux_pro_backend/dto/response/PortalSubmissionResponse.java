package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.FilePriority;
import com.nanotech.flux_pro_backend.enumeration.FileStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PortalSubmissionResponse(
        UUID id,
        String referenceNumber,
        String preconfiguredDossierCode,
        String fileTypeCode,
        String subject,
        FileStatus status,
        FilePriority priority,
        LocalDate receivedAt,
        Map<String, Object> formData,
        /** Schéma du formulaire (libellés) — null si indisponible. */
        Map<String, Object> formSchema,
        String currentStepLabel,
        Integer currentStepOrder,
        List<FileAttachmentResponse> attachments,
        Instant createdAt,
        Instant updatedAt
) {
}
