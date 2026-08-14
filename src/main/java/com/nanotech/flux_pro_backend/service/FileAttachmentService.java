package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.FileException;
import com.nanotech.flux_pro_backend.dto.response.FileAttachmentResponse;
import com.nanotech.flux_pro_backend.entity.FileAttachment;
import com.nanotech.flux_pro_backend.entity.FileEntity;
import com.nanotech.flux_pro_backend.entity.FilePassage;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.AttachmentKind;
import com.nanotech.flux_pro_backend.enumeration.FileStatus;
import com.nanotech.flux_pro_backend.enumeration.PassageStatus;
import com.nanotech.flux_pro_backend.mapper.FileMapper;
import com.nanotech.flux_pro_backend.repository.FileAttachmentRepository;
import com.nanotech.flux_pro_backend.repository.FilePassageRepository;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileAttachmentService {

    static final long MAX_SIZE_BYTES = 21_474_836L;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "image/jpeg",
            "image/png");

    private final FileAttachmentRepository fileAttachmentRepository;
    private final FilePassageRepository filePassageRepository;
    private final UserRepository userRepository;
    private final AttachmentStorageService storageService;
    private final PassageAuthorityService passageAuthorityService;

    @Transactional(readOnly = true)
    public List<FileAttachmentResponse> listForFile(FileEntity file) {
        return fileAttachmentRepository.findByFileIdOrderByCreatedAtAsc(file.getId()).stream()
                .map(FileMapper::toAttachment)
                .toList();
    }

    /**
     * Upload typé :
     * <ul>
     *   <li>{@link AttachmentKind#CREATION} — dossier {@code DRAFT}</li>
     *   <li>{@link AttachmentKind#PASSAGE} — maillon {@code IN_PROGRESS}, acteur autorisé</li>
     *   <li>{@link AttachmentKind#CLOSURE} — dossier {@code IN_PROGRESS}</li>
     * </ul>
     */
    @Transactional
    public FileAttachmentResponse upload(
            FileEntity file,
            MultipartFile multipart,
            AttachmentKind kind,
            UUID passageId,
            Boolean portalVisible,
            SecurityUser actor) {
        AttachmentKind effectiveKind = kind != null ? kind : AttachmentKind.CREATION;
        FilePassage passage = resolveAndAssertUpload(file, effectiveKind, passageId, actor);

        validateFile(multipart);

        User actorUser = userRepository.findById(actor.getId())
                .orElseThrow(() -> FileException.notFound("FILE_USER_NOT_FOUND", "User not found"));
        // Pièce de maillon : attribution métier = responsable du maillon (titulaire),
        // même si un suppléant / admin exécute l'upload.
        User uploader = effectiveKind == AttachmentKind.PASSAGE && passage != null && passage.getResponsibleUser() != null
                ? passage.getResponsibleUser()
                : actorUser;

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

        boolean visible = effectiveKind == AttachmentKind.CLOSURE
                && Boolean.TRUE.equals(portalVisible)
                && file.getPortalUser() != null;

        FileAttachment attachment = new FileAttachment();
        attachment.setFile(file);
        attachment.setOriginalFilename(multipart.getOriginalFilename());
        attachment.setContentType(
                multipart.getContentType() != null ? multipart.getContentType() : "application/octet-stream");
        attachment.setSizeBytes(multipart.getSize());
        attachment.setStorageBucket(storageService.defaultBucket());
        attachment.setStorageKey(storageKey);
        attachment.setAttachmentKind(effectiveKind);
        attachment.setResponseDocument(effectiveKind == AttachmentKind.CLOSURE);
        attachment.setPassage(passage);
        attachment.setPortalVisible(visible);
        attachment.setUploadedBy(uploader);

        return FileMapper.toAttachment(fileAttachmentRepository.save(attachment));
    }

    /** Compat : {@code responseDocument=true} → {@link AttachmentKind#CLOSURE}. */
    @Transactional
    public FileAttachmentResponse upload(
            FileEntity file, MultipartFile multipart, boolean responseDocument, SecurityUser actor) {
        return upload(
                file,
                multipart,
                responseDocument ? AttachmentKind.CLOSURE : AttachmentKind.CREATION,
                null,
                false,
                actor);
    }

    @Transactional
    public FileAttachmentResponse updatePortalVisibility(
            FileEntity file, UUID attachmentId, boolean portalVisible, SecurityUser actor) {
        FileAttachment attachment = fileAttachmentRepository.findByIdAndFileId(attachmentId, file.getId())
                .orElseThrow(() -> FileException.notFound("FILE_ATTACHMENT_NOT_FOUND", "Attachment not found"));
        if (resolveKind(attachment) != AttachmentKind.CLOSURE) {
            throw FileException.badRequest(
                    "FILE_ATTACHMENT_VISIBILITY_KIND",
                    "Only closure attachments can be shown on the portal");
        }
        if (file.getPortalUser() == null) {
            throw FileException.badRequest(
                    "FILE_ATTACHMENT_VISIBILITY_NO_PORTAL",
                    "Portal visibility only applies to portal-originated files");
        }
        if (file.getStatus() != FileStatus.IN_PROGRESS && file.getStatus() != FileStatus.CLOSED) {
            throw FileException.conflict(
                    "FILE_ATTACHMENT_VISIBILITY_LOCKED",
                    "Portal visibility can only be changed on in-progress or closed files");
        }
        attachment.setPortalVisible(portalVisible);
        return FileMapper.toAttachment(fileAttachmentRepository.save(attachment));
    }

    @Transactional
    public void delete(FileEntity file, UUID attachmentId, SecurityUser actor) {
        FileAttachment attachment = fileAttachmentRepository.findByIdAndFileId(attachmentId, file.getId())
                .orElseThrow(() -> FileException.notFound("FILE_ATTACHMENT_NOT_FOUND", "Attachment not found"));
        assertDeleteAllowed(file, attachment, actor);
        try {
            storageService.delete(attachment.getStorageBucket(), attachment.getStorageKey());
        } catch (IOException e) {
            throw FileException.badRequest(
                    "FILE_ATTACHMENT_DELETE_FAILED", "Failed to delete attachment file");
        }
        fileAttachmentRepository.delete(attachment);
    }

    /** Compat ancienne signature (sans acteur) — suppression CREATION en DRAFT uniquement. */
    @Transactional
    public void delete(FileEntity file, UUID attachmentId) {
        if (file.getStatus() != FileStatus.DRAFT) {
            throw FileException.conflict(
                    "FILE_ATTACHMENT_DELETE_LOCKED", "Attachments cannot be deleted after submission");
        }
        FileAttachment attachment = fileAttachmentRepository.findByIdAndFileId(attachmentId, file.getId())
                .orElseThrow(() -> FileException.notFound("FILE_ATTACHMENT_NOT_FOUND", "Attachment not found"));
        try {
            storageService.delete(attachment.getStorageBucket(), attachment.getStorageKey());
        } catch (IOException e) {
            throw FileException.badRequest(
                    "FILE_ATTACHMENT_DELETE_FAILED", "Failed to delete attachment file");
        }
        fileAttachmentRepository.delete(attachment);
    }

    @Transactional(readOnly = true)
    public Resource download(FileEntity file, UUID attachmentId) {
        FileAttachment attachment = fileAttachmentRepository.findByIdAndFileId(attachmentId, file.getId())
                .orElseThrow(() -> FileException.notFound("FILE_ATTACHMENT_NOT_FOUND", "Attachment not found"));
        try {
            return storageService.loadAsResource(attachment.getStorageBucket(), attachment.getStorageKey());
        } catch (IOException e) {
            throw FileException.notFound(
                    "FILE_ATTACHMENT_STORAGE_MISSING", "Attachment file not found on storage");
        }
    }

    @Transactional(readOnly = true)
    public FileAttachment getAttachment(FileEntity file, UUID attachmentId) {
        return fileAttachmentRepository.findByIdAndFileId(attachmentId, file.getId())
                .orElseThrow(() -> FileException.notFound("FILE_ATTACHMENT_NOT_FOUND", "Attachment not found"));
    }

    @Transactional(readOnly = true)
    public String getOriginalFilename(FileEntity file, UUID attachmentId) {
        return getAttachment(file, attachmentId).getOriginalFilename();
    }

    @Transactional(readOnly = true)
    public String getContentType(FileEntity file, UUID attachmentId) {
        return getAttachment(file, attachmentId).getContentType();
    }

    public void validateFile(MultipartFile multipart) {
        if (multipart == null || multipart.isEmpty()) {
            throw FileException.badRequest("FILE_ATTACHMENT_REQUIRED", "Attachment file is required");
        }
        if (multipart.getSize() > MAX_SIZE_BYTES) {
            throw FileException.badRequest(
                    "FILE_ATTACHMENT_TOO_LARGE", "Attachment exceeds maximum size of 20 MB");
        }
        String contentType = multipart.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw FileException.badRequest(
                    "FILE_ATTACHMENT_TYPE_NOT_ALLOWED",
                    "Attachment type not allowed. Allowed: PDF, DOCX, XLSX, JPEG, PNG");
        }
    }

    private FilePassage resolveAndAssertUpload(
            FileEntity file, AttachmentKind kind, UUID passageId, SecurityUser actor) {
        return switch (kind) {
            case CREATION -> {
                if (file.getStatus() != FileStatus.DRAFT) {
                    throw FileException.conflict(
                            "FILE_ATTACHMENT_UPLOAD_LOCKED",
                            "Creation attachments can only be uploaded on draft files");
                }
                yield null;
            }
            case PASSAGE -> {
                if (file.getStatus() != FileStatus.IN_PROGRESS && file.getStatus() != FileStatus.ON_HOLD) {
                    throw FileException.conflict(
                            "FILE_ATTACHMENT_UPLOAD_LOCKED",
                            "Passage attachments require an in-progress file");
                }
                if (passageId == null) {
                    throw FileException.badRequest(
                            "FILE_ATTACHMENT_PASSAGE_REQUIRED", "passageId is required for PASSAGE attachments");
                }
                FilePassage passage = filePassageRepository.findByIdAndFileIdWithDetails(passageId, file.getId())
                        .orElseThrow(() -> FileException.notFound(
                                "FILE_PASSAGE_NOT_FOUND", "Passage not found on this file"));
                if (passage.getStatus() != PassageStatus.IN_PROGRESS
                        && passage.getStatus() != PassageStatus.SUSPENDED) {
                    throw FileException.conflict(
                            "FILE_ATTACHMENT_PASSAGE_INACTIVE",
                            "Attachments can only be added on an active passage");
                }
                if (!passageAuthorityService.canActOnPassage(actor, passage)) {
                    throw FileException.forbidden(
                            "FILE_ATTACHMENT_PASSAGE_FORBIDDEN",
                            "Only the passage responsible (or substitute) can add attachments");
                }
                yield passage;
            }
            case CLOSURE -> {
                if (file.getStatus() != FileStatus.IN_PROGRESS) {
                    throw FileException.conflict(
                            "FILE_ATTACHMENT_UPLOAD_LOCKED",
                            "Closure attachments can only be uploaded on in-progress files");
                }
                yield null;
            }
        };
    }

    private void assertDeleteAllowed(FileEntity file, FileAttachment attachment, SecurityUser actor) {
        AttachmentKind kind = resolveKind(attachment);
        switch (kind) {
            case CREATION -> {
                if (file.getStatus() != FileStatus.DRAFT) {
                    throw FileException.conflict(
                            "FILE_ATTACHMENT_DELETE_LOCKED",
                            "Creation attachments cannot be deleted after submission");
                }
            }
            case PASSAGE -> {
                FilePassage passage = attachment.getPassage();
                if (passage == null
                        || (passage.getStatus() != PassageStatus.IN_PROGRESS
                                && passage.getStatus() != PassageStatus.SUSPENDED)
                        || !passageAuthorityService.canActOnPassage(actor, passage)) {
                    throw FileException.conflict(
                            "FILE_ATTACHMENT_DELETE_LOCKED",
                            "Passage attachments can only be deleted by the active responsible");
                }
            }
            case CLOSURE -> {
                if (file.getStatus() != FileStatus.IN_PROGRESS) {
                    throw FileException.conflict(
                            "FILE_ATTACHMENT_DELETE_LOCKED",
                            "Closure attachments cannot be deleted after the file is closed");
                }
                // Même règle que l'upload : seul un acteur autorisé sur un maillon actif peut retirer.
                boolean canAct = filePassageRepository
                        .findAllByFileIdAndStatusWithDetails(file.getId(), PassageStatus.IN_PROGRESS)
                        .stream()
                        .anyMatch(p -> passageAuthorityService.canActOnPassage(actor, p));
                if (!canAct) {
                    canAct = filePassageRepository
                            .findAllByFileIdAndStatusWithDetails(file.getId(), PassageStatus.SUSPENDED)
                            .stream()
                            .anyMatch(p -> passageAuthorityService.canActOnPassage(actor, p));
                }
                if (!canAct) {
                    throw FileException.forbidden(
                            "FILE_ATTACHMENT_DELETE_FORBIDDEN",
                            "Only the active passage responsible can delete closure attachments");
                }
            }
        }
    }

    private AttachmentKind resolveKind(FileAttachment attachment) {
        if (attachment.getAttachmentKind() != null) {
            return attachment.getAttachmentKind();
        }
        return attachment.isResponseDocument() ? AttachmentKind.CLOSURE : AttachmentKind.CREATION;
    }
}
