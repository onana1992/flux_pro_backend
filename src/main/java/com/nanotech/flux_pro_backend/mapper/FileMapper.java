package com.nanotech.flux_pro_backend.mapper;

import com.nanotech.flux_pro_backend.dto.response.FileAttachmentResponse;
import com.nanotech.flux_pro_backend.dto.response.FileDetailResponse;
import com.nanotech.flux_pro_backend.dto.response.FileSummaryResponse;
import com.nanotech.flux_pro_backend.entity.FileAttachment;
import com.nanotech.flux_pro_backend.entity.FileEntity;
import com.nanotech.flux_pro_backend.entity.PortalUser;
import com.nanotech.flux_pro_backend.entity.User;

import java.util.List;
import java.util.UUID;

public final class FileMapper {

    private FileMapper() {
    }

    public static FileSummaryResponse toSummary(FileEntity file) {
        return new FileSummaryResponse(
                file.getId(),
                file.getReferenceNumber(),
                file.getFileTypeCode(),
                file.getSubject(),
                file.getPriority(),
                file.getStatus(),
                file.getReceivedAt(),
                file.getOrganization() != null ? file.getOrganization().getCode() : null,
                file.getOrganization() != null ? file.getOrganization().getName() : null,
                file.getChainTemplate() != null ? file.getChainTemplate().getCode() : null,
                file.getCreatedAt());
    }

    public static FileDetailResponse toDetail(FileEntity file, List<FileAttachment> attachments) {
        CreatorInfo creator = resolveCreator(file);
        return new FileDetailResponse(
                file.getId(),
                file.getReferenceNumber(),
                file.getFileTypeCode(),
                file.getPreconfiguredDossier() != null ? file.getPreconfiguredDossier().getCode() : null,
                file.getSubject(),
                file.getSenderOrBeneficiary(),
                file.getPriority(),
                file.getStatus(),
                file.getReceivedAt(),
                file.getOrganization() != null ? file.getOrganization().getId() : null,
                file.getOrganization() != null ? file.getOrganization().getCode() : null,
                file.getOrganization() != null ? file.getOrganization().getName() : null,
                file.getChainTemplate() != null ? file.getChainTemplate().getId() : null,
                file.getChainTemplate() != null ? file.getChainTemplate().getCode() : null,
                file.getChainTemplate() != null ? file.getChainTemplate().getName() : null,
                creator.id(),
                creator.name(),
                creator.source(),
                creator.email(),
                creator.phone(),
                creator.staffNumber(),
                creator.role(),
                creator.jobTitle(),
                creator.organizationCode(),
                creator.organizationName(),
                creator.active(),
                file.getClosureReason(),
                file.getClosedAt(),
                file.getCancellationReason(),
                file.getCancelledAt(),
                file.getMetadata(),
                file.getPreconfiguredDossier() != null ? file.getPreconfiguredDossier().getFormSchema() : null,
                attachments.stream().map(FileMapper::toAttachment).toList(),
                file.getCreatedAt(),
                file.getUpdatedAt());
    }

    private static CreatorInfo resolveCreator(FileEntity file) {
        User createdBy = file.getCreatedBy();
        if (createdBy != null) {
            return new CreatorInfo(
                    createdBy.getId(),
                    createdBy.getFirstName() + " " + createdBy.getLastName(),
                    "USER",
                    createdBy.getEmail(),
                    createdBy.getPhone(),
                    createdBy.getStaffNumber(),
                    createdBy.getRole() != null ? createdBy.getRole().name() : null,
                    createdBy.getJobTitle(),
                    createdBy.getOrganization() != null ? createdBy.getOrganization().getCode() : null,
                    createdBy.getOrganization() != null ? createdBy.getOrganization().getName() : null,
                    createdBy.isActive());
        }
        PortalUser portalUser = file.getPortalUser();
        if (portalUser != null) {
            return new CreatorInfo(
                    portalUser.getId(),
                    portalUser.getFirstName() + " " + portalUser.getLastName(),
                    "PORTAL",
                    portalUser.getEmail(),
                    portalUser.getPhone(),
                    portalUser.getStaffNumber(),
                    portalUser.getPortalUserType() != null ? portalUser.getPortalUserType().name() : null,
                    null,
                    portalUser.getOrganization() != null ? portalUser.getOrganization().getCode() : null,
                    portalUser.getOrganization() != null ? portalUser.getOrganization().getName() : null,
                    portalUser.isActive());
        }
        return CreatorInfo.empty();
    }

    private record CreatorInfo(
            UUID id,
            String name,
            String source,
            String email,
            String phone,
            String staffNumber,
            String role,
            String jobTitle,
            String organizationCode,
            String organizationName,
            Boolean active) {
        static CreatorInfo empty() {
            return new CreatorInfo(null, null, null, null, null, null, null, null, null, null, null);
        }
    }

    public static FileAttachmentResponse toAttachment(FileAttachment attachment) {
        String uploaderName = null;
        UUID uploadedById = null;
        if (attachment.getUploadedBy() != null) {
            uploadedById = attachment.getUploadedBy().getId();
            uploaderName = attachment.getUploadedBy().getFirstName() + " " + attachment.getUploadedBy().getLastName();
        } else if (attachment.getUploadedByPortalUser() != null) {
            uploadedById = attachment.getUploadedByPortalUser().getId();
            uploaderName = attachment.getUploadedByPortalUser().getFirstName()
                    + " " + attachment.getUploadedByPortalUser().getLastName();
        }
        return new FileAttachmentResponse(
                attachment.getId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.isResponseDocument(),
                uploadedById,
                uploaderName,
                attachment.getCreatedAt());
    }
}
