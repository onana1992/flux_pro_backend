package com.nanotech.flux_pro_backend.service.portal;

import com.nanotech.flux_pro_backend.common.AppException;
import com.nanotech.flux_pro_backend.common.FileException;
import com.nanotech.flux_pro_backend.dto.request.PortalSubmissionCreateRequest;
import com.nanotech.flux_pro_backend.dto.response.FileAttachmentResponse;
import com.nanotech.flux_pro_backend.dto.response.PortalFormSchemaResponse;
import com.nanotech.flux_pro_backend.dto.response.PortalFormTypeResponse;
import com.nanotech.flux_pro_backend.dto.response.PortalSubmissionResponse;
import com.nanotech.flux_pro_backend.entity.ChainTemplate;
import com.nanotech.flux_pro_backend.entity.FileAttachment;
import com.nanotech.flux_pro_backend.entity.FileEntity;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.PortalUser;
import com.nanotech.flux_pro_backend.entity.PreconfiguredDossier;
import com.nanotech.flux_pro_backend.enumeration.AttachmentKind;
import com.nanotech.flux_pro_backend.enumeration.FilePriority;
import com.nanotech.flux_pro_backend.enumeration.FileStatus;
import com.nanotech.flux_pro_backend.enumeration.PassageStatus;
import com.nanotech.flux_pro_backend.enumeration.PortalUserType;
import com.nanotech.flux_pro_backend.mapper.FileMapper;
import com.nanotech.flux_pro_backend.repository.FileAttachmentRepository;
import com.nanotech.flux_pro_backend.repository.FilePassageRepository;
import com.nanotech.flux_pro_backend.repository.FileRepository;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import com.nanotech.flux_pro_backend.repository.PortalUserRepository;
import com.nanotech.flux_pro_backend.repository.PreconfiguredDossierRepository;
import com.nanotech.flux_pro_backend.security.PortalSecurityUser;
import com.nanotech.flux_pro_backend.service.AttachmentStorageService;
import com.nanotech.flux_pro_backend.service.ClockService;
import com.nanotech.flux_pro_backend.service.FileAttachmentService;
import com.nanotech.flux_pro_backend.service.FileService;
import com.nanotech.flux_pro_backend.service.PassageService;
import com.nanotech.flux_pro_backend.service.PreconfiguredDossierService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortalSubmissionService {

    private final PreconfiguredDossierRepository preconfiguredDossierRepository;
    private final FileRepository fileRepository;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final FilePassageRepository filePassageRepository;
    private final PortalUserRepository portalUserRepository;
    private final OrganizationRepository organizationRepository;
    private final FormSchemaValidator formSchemaValidator;
    private final FileService fileService;
    private final PassageService passageService;
    private final FileAttachmentService fileAttachmentService;
    private final AttachmentStorageService storageService;
    private final ClockService clockService;

    @Transactional(readOnly = true)
    public List<PortalFormTypeResponse> listFormTypes(PortalSecurityUser actor) {
        assertCanUsePortal(actor);
        return preconfiguredDossierRepository.findPortalEnabled().stream()
                .filter(d -> isAudienceVisible(actor.getPortalUserType(), d))
                .map(d -> new PortalFormTypeResponse(
                        d.getCode(),
                        d.getName(),
                        d.getNameEn(),
                        d.getDescription(),
                        d.getPortalAudience(),
                        d.getRequiredAttachmentKeys() != null
                                ? d.getRequiredAttachmentKeys()
                                : List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PortalFormSchemaResponse getFormSchema(String code, PortalSecurityUser actor) {
        assertCanUsePortal(actor);
        PreconfiguredDossier dossier = loadPortalDossier(code);
        formSchemaValidator.assertAudienceCompatible(actor.getPortalUserType(), dossier.getPortalAudience());
        return new PortalFormSchemaResponse(
                dossier.getCode(),
                dossier.getName(),
                dossier.getPortalAudience(),
                dossier.getFormSchema(),
                dossier.getRequiredAttachmentKeys() != null
                        ? dossier.getRequiredAttachmentKeys()
                        : List.of());
    }

    @Transactional
    public PortalSubmissionResponse create(PortalSubmissionCreateRequest request, PortalSecurityUser actor) {
        assertCanUsePortal(actor);
        PortalUser portalUser = loadPortalUser(actor.getId());
        PreconfiguredDossier dossier = loadPortalDossier(request.preconfiguredDossierCode());
        formSchemaValidator.assertAudienceCompatible(actor.getPortalUserType(), dossier.getPortalAudience());
        assertPortalConfigReady(dossier);

        var validatedForm = formSchemaValidator.validate(dossier.getFormSchema(), request.formData());

        Organization organization = resolveOrganization(portalUser, dossier);
        String subject = request.subject() != null && !request.subject().isBlank()
                ? request.subject().trim()
                : dossier.getName();
        String sender = (portalUser.getFirstName() + " " + portalUser.getLastName()).trim();
        FilePriority priority = request.priority() != null ? request.priority() : FilePriority.NORMAL;

        FileEntity file = new FileEntity();
        file.setFileTypeCode(dossier.getFileTypeCode());
        file.setPreconfiguredDossier(dossier);
        file.setOrganization(organization);
        file.setCreatedBy(null);
        file.setPortalUser(portalUser);
        file.setSubject(subject);
        file.setSenderOrBeneficiary(sender);
        file.setReceivedAt(clockService.nowZoned().toLocalDate());
        file.setPriority(priority);
        file.setMetadata(validatedForm);
        file.setStatus(FileStatus.DRAFT);
        file = fileRepository.save(file);

        boolean submit = request.submit() == null || request.submit();
        if (submit) {
            return submitInternal(file, dossier);
        }
        return toResponse(file);
    }

    @Transactional
    public PortalSubmissionResponse submit(UUID submissionId, PortalSecurityUser actor) {
        assertCanUsePortal(actor);
        FileEntity file = fileRepository.findByIdAndPortalUserId(submissionId, actor.getId())
                .orElseThrow(() -> AppException.notFound("PORTAL_SUBMISSION_NOT_FOUND", "Submission not found"));
        PreconfiguredDossier dossier = resolveDossierForFile(file);
        return submitInternal(file, dossier);
    }

    @Transactional
    public FileAttachmentResponse uploadAttachment(
            UUID submissionId, String attachmentKey, MultipartFile multipart, PortalSecurityUser actor) {
        assertCanUsePortal(actor);
        FileEntity file = fileRepository.findByIdAndPortalUserId(submissionId, actor.getId())
                .orElseThrow(() -> AppException.notFound("PORTAL_SUBMISSION_NOT_FOUND", "Submission not found"));
        if (file.getStatus() != FileStatus.DRAFT) {
            throw FileException.conflict(
                    "FILE_ATTACHMENT_UPLOAD_LOCKED", "Attachments can only be uploaded on draft files");
        }
        fileAttachmentService.validateFile(multipart);

        PortalUser portalUser = loadPortalUser(actor.getId());
        String storageKey;
        try {
            storageKey = storageService.store(
                    file.getOrganization(),
                    file.getId(),
                    multipart.getOriginalFilename(),
                    multipart.getInputStream(),
                    multipart.getSize(),
                    multipart.getContentType());
        } catch (IOException e) {
            throw FileException.badRequest(
                    "FILE_ATTACHMENT_STORE_FAILED", "Failed to store attachment: " + e.getMessage(),
                    e.getMessage());
        }

        FileAttachment attachment = new FileAttachment();
        attachment.setFile(file);
        attachment.setOriginalFilename(multipart.getOriginalFilename());
        attachment.setContentType(
                multipart.getContentType() != null ? multipart.getContentType() : "application/octet-stream");
        attachment.setSizeBytes(multipart.getSize());
        attachment.setStorageBucket(storageService.defaultBucket());
        attachment.setStorageKey(storageKey);
        attachment.setAttachmentKind(AttachmentKind.CREATION);
        attachment.setResponseDocument(false);
        attachment.setPortalVisible(false);
        attachment.setUploadedBy(null);
        attachment.setUploadedByPortalUser(portalUser);
        if (attachmentKey != null && !attachmentKey.isBlank()) {
            attachment.setPortalAttachmentKey(attachmentKey.trim());
        }
        return FileMapper.toAttachment(fileAttachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public Page<PortalSubmissionResponse> listMine(PortalSecurityUser actor, Pageable pageable) {
        assertCanUsePortal(actor);
        return fileRepository.findByPortalUserIdOrderByCreatedAtDesc(actor.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PortalSubmissionResponse getByRef(String reference, PortalSecurityUser actor) {
        assertCanUsePortal(actor);
        FileEntity file = fileRepository
                .findByReferenceNumberIgnoreCaseAndPortalUserId(reference, actor.getId())
                .orElseThrow(() -> AppException.notFound(
                        "PORTAL_SUBMISSION_NOT_FOUND", "Submission not found: " + reference, reference));
        return toResponse(loadDetails(file.getId()));
    }

    @Transactional(readOnly = true)
    public PortalSubmissionResponse getById(UUID id, PortalSecurityUser actor) {
        assertCanUsePortal(actor);
        FileEntity file = fileRepository.findByIdAndPortalUserId(id, actor.getId())
                .orElseThrow(() -> AppException.notFound("PORTAL_SUBMISSION_NOT_FOUND", "Submission not found"));
        return toResponse(loadDetails(file.getId()));
    }

    private PortalSubmissionResponse submitInternal(FileEntity file, PreconfiguredDossier dossier) {
        if (file.getStatus() != FileStatus.DRAFT) {
            throw FileException.conflict(
                    "FILE_STATUS_SUBMIT_INVALID", "Only draft files can be submitted");
        }
        assertRequiredAttachments(file, dossier);
        assertPortalConfigReady(dossier);

        String reference = fileService.allocateReferenceNumber(file.getOrganization().getId());
        if (fileRepository.existsByReferenceNumberIgnoreCase(reference)) {
            throw FileException.conflict(
                    "FILE_REFERENCE_EXISTS", "Reference number already exists: " + reference, reference);
        }
        file.setReferenceNumber(reference);
        file.setStatus(FileStatus.IN_PROGRESS);
        file = fileRepository.save(file);

        ChainTemplate chain = dossier.getChainTemplate();
        if (chain == null || !chain.isActive()) {
            throw AppException.badRequest(
                    "PORTAL_CHAIN_REQUIRED",
                    "No active chain template linked to preconfigured dossier: " + dossier.getCode(),
                    dossier.getCode());
        }
        passageService.initializeChainForPortalSubmission(
                file,
                chain,
                PreconfiguredDossierService.parseStepAssignments(dossier.getStepAssignments()));

        return toResponse(loadDetails(file.getId()));
    }

    private void assertRequiredAttachments(FileEntity file, PreconfiguredDossier dossier) {
        List<String> keys = dossier.getRequiredAttachmentKeys();
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            long count = fileAttachmentRepository.countByFileIdAndPortalAttachmentKey(file.getId(), key.trim());
            if (count < 1) {
                throw AppException.badRequest(
                        "PORTAL_ATTACHMENT_REQUIRED",
                        "Required attachment missing: " + key,
                        key);
            }
        }
    }

    private void assertPortalConfigReady(PreconfiguredDossier dossier) {
        if (!dossier.isPortalEnabled()) {
            throw AppException.badRequest("PORTAL_TYPE_DISABLED", "Preconfigured dossier is not open on the portal");
        }
        if (dossier.getFormSchema() == null || dossier.getFormSchema().isEmpty()) {
            throw AppException.badRequest("PORTAL_FORM_SCHEMA_MISSING", "Portal form schema is missing");
        }
        if (dossier.getChainTemplate() == null || !dossier.getChainTemplate().isActive()) {
            throw AppException.badRequest(
                    "PORTAL_CHAIN_REQUIRED",
                    "An active chain template must be linked to: " + dossier.getCode(),
                    dossier.getCode());
        }
        Map<UUID, UUID> assignments = PreconfiguredDossierService.parseStepAssignments(dossier.getStepAssignments());
        if (assignments.isEmpty()) {
            throw AppException.badRequest(
                    "PORTAL_RESPONSIBLE_REQUIRED",
                    "Step responsibles must be configured for: " + dossier.getCode(),
                    dossier.getCode());
        }
    }

    private PreconfiguredDossier loadPortalDossier(String code) {
        PreconfiguredDossier dossier = preconfiguredDossierRepository.findByCodeIgnoreCaseWithChain(code.trim())
                .orElseThrow(() -> AppException.notFound(
                        "PORTAL_TYPE_NOT_FOUND", "Portal preconfigured dossier not found: " + code, code));
        if (!dossier.isActive() || !dossier.isPortalEnabled()) {
            throw AppException.notFound(
                    "PORTAL_TYPE_NOT_FOUND", "Portal preconfigured dossier not found: " + code, code);
        }
        return dossier;
    }

    private PreconfiguredDossier resolveDossierForFile(FileEntity file) {
        if (file.getPreconfiguredDossier() != null) {
            return preconfiguredDossierRepository.findByIdWithChain(file.getPreconfiguredDossier().getId())
                    .orElseThrow(() -> AppException.notFound(
                            "PRECONFIGURED_DOSSIER_NOT_FOUND", "Preconfigured dossier not found"));
        }
        // Fallback migration / anciens brouillons : tenter par code type
        return loadPortalDossier(file.getFileTypeCode());
    }

    private Organization resolveOrganization(PortalUser portalUser, PreconfiguredDossier dossier) {
        if (portalUser.getOrganization() != null && portalUser.getOrganization().isActive()) {
            return portalUser.getOrganization();
        }
        if (dossier.getDirectionCode() != null && !dossier.getDirectionCode().isBlank()) {
            return organizationRepository.findByCode(dossier.getDirectionCode().trim())
                    .filter(Organization::isActive)
                    .orElseGet(() -> organizationRepository.findByCode("MINTP")
                            .filter(Organization::isActive)
                            .orElseThrow(() -> AppException.badRequest(
                                    "PORTAL_ORGANIZATION_UNRESOLVED",
                                    "Cannot resolve organization for portal submission")));
        }
        return organizationRepository.findByCode("MINTP")
                .filter(Organization::isActive)
                .orElseThrow(() -> AppException.badRequest(
                        "PORTAL_ORGANIZATION_UNRESOLVED",
                        "Cannot resolve organization for portal submission"));
    }

    private boolean isAudienceVisible(PortalUserType userType, PreconfiguredDossier dossier) {
        try {
            formSchemaValidator.assertAudienceCompatible(userType, dossier.getPortalAudience());
            return true;
        } catch (AppException e) {
            return false;
        }
    }

    private void assertCanUsePortal(PortalSecurityUser actor) {
        if (actor.isMustChangePassword()) {
            throw AppException.forbidden(
                    "PORTAL_MUST_CHANGE_PASSWORD",
                    "You must change your password before using the portal");
        }
        if (actor.getPortalUserType() == PortalUserType.EXTERNAL && !actor.isEmailVerified()) {
            throw AppException.forbidden(
                    "PORTAL_EMAIL_NOT_VERIFIED",
                    "Email must be verified before using the portal");
        }
    }

    private PortalUser loadPortalUser(UUID id) {
        return portalUserRepository.findByIdWithOrganization(id)
                .orElseThrow(() -> AppException.notFound("PORTAL_USER_NOT_FOUND", "Portal user not found"));
    }

    private FileEntity loadDetails(UUID id) {
        return fileRepository.findByIdWithDetails(id)
                .orElseThrow(() -> AppException.notFound("PORTAL_SUBMISSION_NOT_FOUND", "Submission not found"));
    }

    private PortalSubmissionResponse toResponse(FileEntity file) {
        var attachments = fileAttachmentRepository.findByFileIdOrderByCreatedAtAsc(file.getId()).stream()
                .filter(this::isVisibleOnPortal)
                .map(FileMapper::toAttachment)
                .toList();
        String stepLabel = null;
        Integer stepOrder = null;
        var active = filePassageRepository.findAllByFileIdAndStatusWithDetails(
                file.getId(), PassageStatus.IN_PROGRESS);
        if (!active.isEmpty()) {
            var passage = active.get(0);
            stepOrder = passage.getStepOrder();
            if (passage.getChainStepTemplate() != null) {
                stepLabel = passage.getChainStepTemplate().getLabel();
            }
        }
        String dossierCode = file.getPreconfiguredDossier() != null
                ? file.getPreconfiguredDossier().getCode()
                : file.getFileTypeCode();
        Map<String, Object> formSchema = file.getPreconfiguredDossier() != null
                ? file.getPreconfiguredDossier().getFormSchema()
                : null;
        return new PortalSubmissionResponse(
                file.getId(),
                file.getReferenceNumber(),
                dossierCode,
                file.getFileTypeCode(),
                file.getSubject(),
                file.getStatus(),
                file.getPriority(),
                file.getReceivedAt(),
                file.getMetadata(),
                formSchema,
                stepLabel,
                stepOrder,
                attachments,
                file.getCreatedAt(),
                file.getUpdatedAt());
    }

    /**
     * Téléchargement d'une PJ visible sur le portail (CREATION toujours ;
     * CLOSURE seulement si {@code portalVisible}).
     */
    @Transactional(readOnly = true)
    public PortalAttachmentDownload downloadAttachment(
            UUID submissionId, UUID attachmentId, PortalSecurityUser actor) {
        FileEntity file = loadOwnedSubmission(submissionId, actor);
        FileAttachment attachment = requireVisibleAttachment(file, attachmentId);
        Resource resource = fileAttachmentService.download(file, attachment.getId());
        return new PortalAttachmentDownload(
                resource,
                attachment.getOriginalFilename(),
                attachment.getContentType() != null && !attachment.getContentType().isBlank()
                        ? attachment.getContentType()
                        : "application/octet-stream");
    }

    public record PortalAttachmentDownload(Resource resource, String filename, String contentType) {}

    private FileEntity loadOwnedSubmission(UUID submissionId, PortalSecurityUser actor) {
        assertCanUsePortal(actor);
        return fileRepository.findByIdAndPortalUserId(submissionId, actor.getId())
                .orElseThrow(() -> AppException.notFound("PORTAL_SUBMISSION_NOT_FOUND", "Submission not found"));
    }

    private FileAttachment requireVisibleAttachment(FileEntity file, UUID attachmentId) {
        FileAttachment attachment = fileAttachmentRepository.findByIdAndFileId(attachmentId, file.getId())
                .orElseThrow(() -> AppException.notFound("FILE_ATTACHMENT_NOT_FOUND", "Attachment not found"));
        if (!isVisibleOnPortal(attachment)) {
            throw AppException.notFound("FILE_ATTACHMENT_NOT_FOUND", "Attachment not found");
        }
        return attachment;
    }

    /** CREATION toujours (demandeur) ; CLOSURE seulement si portalVisible ; PASSAGE jamais. */
    private boolean isVisibleOnPortal(FileAttachment attachment) {
        AttachmentKind kind = attachment.getAttachmentKind() != null
                ? attachment.getAttachmentKind()
                : (attachment.isResponseDocument() ? AttachmentKind.CLOSURE : AttachmentKind.CREATION);
        return switch (kind) {
            case CREATION -> true;
            case CLOSURE -> attachment.isPortalVisible();
            case PASSAGE -> false;
        };
    }
}
