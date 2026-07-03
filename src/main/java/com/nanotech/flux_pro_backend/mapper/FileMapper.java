package com.nanotech.flux_pro_backend.mapper;

import com.nanotech.flux_pro_backend.dto.response.FileAttachmentResponse;
import com.nanotech.flux_pro_backend.dto.response.FileDetailResponse;
import com.nanotech.flux_pro_backend.dto.response.FileSummaryResponse;
import com.nanotech.flux_pro_backend.entity.FileAttachment;
import com.nanotech.flux_pro_backend.entity.FileEntity;

import java.util.List;

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
        String createdByName = file.getCreatedBy() != null
                ? file.getCreatedBy().getFirstName() + " " + file.getCreatedBy().getLastName()
                : null;
        return new FileDetailResponse(
                file.getId(),
                file.getReferenceNumber(),
                file.getFileTypeCode(),
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
                file.getCreatedBy() != null ? file.getCreatedBy().getId() : null,
                createdByName,
                file.getClosureReason(),
                file.getClosedAt(),
                file.getCancellationReason(),
                file.getCancelledAt(),
                file.getMetadata(),
                attachments.stream().map(FileMapper::toAttachment).toList(),
                file.getCreatedAt(),
                file.getUpdatedAt());
    }

    public static FileAttachmentResponse toAttachment(FileAttachment attachment) {
        String uploaderName = attachment.getUploadedBy() != null
                ? attachment.getUploadedBy().getFirstName() + " " + attachment.getUploadedBy().getLastName()
                : null;
        return new FileAttachmentResponse(
                attachment.getId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.isResponseDocument(),
                attachment.getUploadedBy() != null ? attachment.getUploadedBy().getId() : null,
                uploaderName,
                attachment.getCreatedAt());
    }
}
