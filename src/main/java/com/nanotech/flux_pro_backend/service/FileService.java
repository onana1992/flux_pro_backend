package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.FileException;
import com.nanotech.flux_pro_backend.dto.request.FileCancelRequest;
import com.nanotech.flux_pro_backend.dto.request.FileCloseRequest;
import com.nanotech.flux_pro_backend.dto.request.FileCreateRequest;
import com.nanotech.flux_pro_backend.dto.request.FileUpdateRequest;
import com.nanotech.flux_pro_backend.dto.response.FileDetailResponse;
import com.nanotech.flux_pro_backend.dto.response.FileSummaryResponse;
import com.nanotech.flux_pro_backend.entity.ChainTemplate;
import com.nanotech.flux_pro_backend.entity.FileEntity;
import com.nanotech.flux_pro_backend.entity.FileNumberSequence;
import com.nanotech.flux_pro_backend.entity.FileType;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.FilePriority;
import com.nanotech.flux_pro_backend.enumeration.FileStatus;
import com.nanotech.flux_pro_backend.mapper.FileMapper;
import com.nanotech.flux_pro_backend.repository.ChainTemplateRepository;
import com.nanotech.flux_pro_backend.repository.FileAttachmentRepository;
import com.nanotech.flux_pro_backend.repository.FileNumberSequenceRepository;
import com.nanotech.flux_pro_backend.repository.FileRepository;
import com.nanotech.flux_pro_backend.repository.FileTypeRepository;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import com.nanotech.flux_pro_backend.security.AccessControlService;
import com.nanotech.flux_pro_backend.security.OrganizationScopeService;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    static final String VERY_URGENT_COURIER_TYPE = "COUR-STD";
    static final String VERY_URGENT_TEMPLATE_CODE = "T02";

    private final FileRepository fileRepository;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final FileNumberSequenceRepository fileNumberSequenceRepository;
    private final FileTypeRepository fileTypeRepository;
    private final ChainTemplateRepository chainTemplateRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final AccessControlService accessControlService;
    private final OrganizationScopeService organizationScopeService;
    private final ClockService clockService;
    private final TenantSettingsService tenantSettingsService;

    @Transactional(readOnly = true)
    public Page<FileSummaryResponse> findAll(
            String search,
            UUID organizationId,
            String fileTypeCode,
            FileStatus status,
            FilePriority priority,
            LocalDate receivedFrom,
            LocalDate receivedTo,
            Pageable pageable,
            SecurityUser actor) {
        boolean allAccessible = organizationScopeService.hasGlobalScope(actor);
        return fileRepository.search(
                        allAccessible,
                        actor.getId(),
                        organizationId,
                        fileTypeCode,
                        status,
                        priority,
                        receivedFrom,
                        receivedTo,
                        search,
                        pageable)
                .map(FileMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public FileDetailResponse findById(UUID id, SecurityUser actor) {
        FileEntity file = loadWithDetails(id);
        accessControlService.assertCanAccessFile(actor, file);
        return toDetail(file);
    }

    @Transactional(readOnly = true)
    public FileDetailResponse findByReference(String reference, SecurityUser actor) {
        FileEntity file = fileRepository.findByReferenceNumberIgnoreCase(reference)
                .orElseThrow(() -> FileException.notFound(
                        "FILE_NOT_FOUND_BY_REFERENCE", "File not found: " + reference, reference));
        accessControlService.assertCanAccessFile(actor, file);
        return toDetail(loadWithDetails(file.getId()));
    }

    @Transactional
    public FileDetailResponse create(FileCreateRequest request, SecurityUser actor) {
        accessControlService.assertCanAccessOrganization(actor, request.organizationId());
        validateFileType(request.fileTypeCode());

        Organization organization = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> FileException.badRequest("FILE_ORGANIZATION_NOT_FOUND", "Organization not found"));
        if (!organization.isActive()) {
            throw FileException.badRequest("FILE_ORGANIZATION_INACTIVE", "Organization is inactive");
        }

        User creator = userRepository.findById(actor.getId())
                .orElseThrow(() -> FileException.notFound("FILE_USER_NOT_FOUND", "User not found"));

        FileEntity file = new FileEntity();
        file.setFileTypeCode(request.fileTypeCode().trim().toUpperCase());
        file.setOrganization(organization);
        file.setCreatedBy(creator);
        applyMetadata(file, request.subject(), request.senderOrBeneficiary(),
                request.receivedAt(), request.priority(), request.metadata());
        file.setStatus(FileStatus.DRAFT);

        file = fileRepository.save(file);

        if (request.submit()) {
            return submitInternal(file, actor);
        }
        return toDetail(file);
    }

    @Transactional
    public FileDetailResponse update(UUID id, FileUpdateRequest request, SecurityUser actor) {
        FileEntity file = loadWithDetails(id);
        accessControlService.assertCanAccessFile(actor, file);
        assertEditable(file);

        accessControlService.assertCanAccessOrganization(actor, request.organizationId());
        validateFileType(request.fileTypeCode());

        Organization organization = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> FileException.badRequest("FILE_ORGANIZATION_NOT_FOUND", "Organization not found"));
        if (!organization.isActive()) {
            throw FileException.badRequest("FILE_ORGANIZATION_INACTIVE", "Organization is inactive");
        }

        file.setFileTypeCode(request.fileTypeCode().trim().toUpperCase());
        file.setOrganization(organization);
        applyMetadata(file, request.subject(), request.senderOrBeneficiary(),
                request.receivedAt(), request.priority(), request.metadata());

        return toDetail(fileRepository.save(file));
    }

    @Transactional
    public FileDetailResponse submit(UUID id, SecurityUser actor) {
        FileEntity file = loadWithDetails(id);
        accessControlService.assertCanAccessFile(actor, file);
        return submitInternal(file, actor);
    }

    @Transactional
    public FileDetailResponse cancel(UUID id, FileCancelRequest request, SecurityUser actor) {
        FileEntity file = loadWithDetails(id);
        accessControlService.assertCanAccessFile(actor, file);
        if (file.getStatus() != FileStatus.DRAFT && file.getStatus() != FileStatus.IN_PROGRESS) {
            throw FileException.conflict(
                    "FILE_STATUS_CANCEL_INVALID",
                    "File cannot be cancelled in status " + file.getStatus(), file.getStatus());
        }
        file.setStatus(FileStatus.CANCELLED);
        file.setCancellationReason(request.reason());
        file.setCancelledAt(clockService.now());
        return toDetail(fileRepository.save(file));
    }

    @Transactional
    public FileDetailResponse close(UUID id, FileCloseRequest request, SecurityUser actor) {
        FileEntity file = loadWithDetails(id);
        accessControlService.assertCanAccessFile(actor, file);
        if (file.getStatus() != FileStatus.IN_PROGRESS) {
            throw FileException.conflict(
                    "FILE_STATUS_CLOSE_INVALID",
                    "File can only be closed when in progress");
        }

        if (request.responseAttachmentId() != null) {
            var attachment = fileAttachmentRepository.findByIdAndFileId(request.responseAttachmentId(), file.getId())
                    .orElseThrow(() -> FileException.badRequest(
                            "FILE_CLOSURE_INCOMPLETE", "Response attachment not found on this file"));
            if (!attachment.isResponseDocument()) {
                attachment.setResponseDocument(true);
                fileAttachmentRepository.save(attachment);
            }
        }

        file.setClosureReason(request.closureReason());
        file.setClosedAt(clockService.now());
        file.setStatus(FileStatus.CLOSED);
        return toDetail(fileRepository.save(file));
    }

    @Transactional
    public FileDetailResponse archive(UUID id, SecurityUser actor) {
        FileEntity file = loadWithDetails(id);
        accessControlService.assertCanAccessFile(actor, file);
        if (file.getStatus() != FileStatus.CLOSED) {
            throw FileException.conflict(
                    "FILE_STATUS_ARCHIVE_INVALID",
                    "Only closed files can be archived");
        }
        file.setStatus(FileStatus.ARCHIVED);
        return toDetail(fileRepository.save(file));
    }

    @Transactional
    public void deleteDraft(UUID id, SecurityUser actor) {
        FileEntity file = loadWithDetails(id);
        accessControlService.assertCanAccessFile(actor, file);
        if (file.getStatus() != FileStatus.DRAFT) {
            throw FileException.conflict("FILE_DELETE_FORBIDDEN", "Only draft files can be deleted");
        }
        fileRepository.delete(file);
    }

    @Transactional(readOnly = true)
    public FileEntity loadForAttachment(UUID id, SecurityUser actor) {
        FileEntity file = loadWithDetails(id);
        accessControlService.assertCanAccessFile(actor, file);
        return file;
    }

    @Transactional
    public String allocateReferenceNumber(UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> FileException.badRequest("FILE_ORGANIZATION_NOT_FOUND", "Organization not found"));

        int year = clockService.nowZoned().getYear();
        FileNumberSequence sequence = fileNumberSequenceRepository.findForUpdate(organizationId, year)
                .orElseGet(() -> {
                    FileNumberSequence created = new FileNumberSequence();
                    created.setOrganizationId(organizationId);
                    created.setYear(year);
                    created.setLastSequence(0);
                    return fileNumberSequenceRepository.save(created);
                });

        int next = sequence.getLastSequence() + 1;
        sequence.setLastSequence(next);
        fileNumberSequenceRepository.save(sequence);

        return tenantSettingsService.referencePrefix()
                + "-" + organization.getCode() + "-" + year + "-" + String.format("%04d", next);
    }

    public ChainTemplate resolveTemplate(String fileTypeCode, FilePriority priority) {
        validateFileType(fileTypeCode);

        if (priority == FilePriority.VERY_URGENT
                && VERY_URGENT_COURIER_TYPE.equalsIgnoreCase(fileTypeCode)) {
            return chainTemplateRepository.findByCodeIgnoreCase(VERY_URGENT_TEMPLATE_CODE)
                    .filter(ChainTemplate::isActive)
                    .orElseThrow(() -> FileException.badRequest(
                            "FILE_TEMPLATE_NOT_FOUND_URGENT",
                            "No active chain template found for very urgent courier"));
        }

        return chainTemplateRepository
                .findFirstByFileTypeCodeIgnoreCaseAndActiveTrue(fileTypeCode.trim())
                .orElseThrow(() -> FileException.badRequest(
                        "FILE_TEMPLATE_NOT_FOUND_BY_TYPE",
                        "No active chain template found for file type: " + fileTypeCode, fileTypeCode));
    }

    private FileDetailResponse submitInternal(FileEntity file, SecurityUser actor) {
        if (file.getStatus() != FileStatus.DRAFT) {
            throw FileException.conflict(
                    "FILE_STATUS_SUBMIT_INVALID",
                    "Only draft files can be submitted");
        }

        String reference = allocateReferenceNumber(file.getOrganization().getId());

        if (fileRepository.existsByReferenceNumberIgnoreCase(reference)) {
            throw FileException.conflict(
                    "FILE_REFERENCE_EXISTS", "Reference number already exists: " + reference, reference);
        }

        file.setReferenceNumber(reference);
        file.setStatus(FileStatus.IN_PROGRESS);
        file = fileRepository.save(file);
        return toDetail(file);
    }

    private void applyMetadata(
            FileEntity file,
            String subject,
            String senderOrBeneficiary,
            LocalDate receivedAt,
            FilePriority priority,
            java.util.Map<String, Object> metadata) {
        file.setSubject(subject.trim());
        file.setSenderOrBeneficiary(senderOrBeneficiary.trim());
        file.setReceivedAt(receivedAt);
        file.setPriority(priority);
        file.setMetadata(metadata);
    }

    private void validateFileType(String fileTypeCode) {
        FileType fileType = fileTypeRepository.findByCodeIgnoreCase(fileTypeCode.trim())
                .orElseThrow(() -> FileException.badRequest(
                        "FILE_TYPE_INVALID", "File type not found or inactive: " + fileTypeCode, fileTypeCode));
        if (!fileType.isActive()) {
            throw FileException.badRequest(
                    "FILE_TYPE_INVALID", "File type not found or inactive: " + fileTypeCode, fileTypeCode);
        }
    }

    private void assertEditable(FileEntity file) {
        if (file.getStatus() != FileStatus.DRAFT) {
            throw FileException.conflict(
                    "FILE_NOT_EDITABLE",
                    "File cannot be edited in status " + file.getStatus(), file.getStatus());
        }
    }

    private FileEntity loadWithDetails(UUID id) {
        return fileRepository.findByIdWithDetails(id)
                .orElseThrow(() -> FileException.notFound("FILE_NOT_FOUND", "File not found"));
    }

    private FileDetailResponse toDetail(FileEntity file) {
        var attachments = fileAttachmentRepository.findByFileIdOrderByCreatedAtAsc(file.getId());
        return FileMapper.toDetail(file, attachments);
    }
}
