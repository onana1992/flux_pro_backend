package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.FilePriority;
import com.nanotech.flux_pro_backend.enumeration.FileStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FileDetailResponse(
        UUID id,
        String referenceNumber,
        String fileTypeCode,
        String preconfiguredDossierCode,
        String subject,
        String senderOrBeneficiary,
        FilePriority priority,
        FileStatus status,
        LocalDate receivedAt,
        UUID organizationId,
        String organizationCode,
        String organizationName,
        UUID chainTemplateId,
        String chainTemplateCode,
        String chainTemplateName,
        UUID createdByUserId,
        String createdByName,
        /** USER = agent interne, PORTAL = utilisateur portail. */
        String createdBySource,
        String createdByEmail,
        String createdByPhone,
        String createdByStaffNumber,
        String createdByRole,
        String createdByJobTitle,
        String createdByOrganizationCode,
        String createdByOrganizationName,
        Boolean createdByActive,
        String closureReason,
        Instant closedAt,
        String cancellationReason,
        Instant cancelledAt,
        Map<String, Object> metadata,
        /** Schéma du formulaire préconfiguré (libellés) — null si dossier hors portail. */
        Map<String, Object> formSchema,
        List<FileAttachmentResponse> attachments,
        Instant createdAt,
        Instant updatedAt
) {
}
