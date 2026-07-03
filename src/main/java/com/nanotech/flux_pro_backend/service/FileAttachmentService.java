package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.FileException;
import com.nanotech.flux_pro_backend.dto.response.FileAttachmentResponse;
import com.nanotech.flux_pro_backend.entity.FileAttachment;
import com.nanotech.flux_pro_backend.entity.FileEntity;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.FileStatus;
import com.nanotech.flux_pro_backend.mapper.FileMapper;
import com.nanotech.flux_pro_backend.repository.FileAttachmentRepository;
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
    private final UserRepository userRepository;
    private final LocalAttachmentStorageService storageService;

    @Transactional(readOnly = true)
    public List<FileAttachmentResponse> listForFile(FileEntity file) {
        return fileAttachmentRepository.findByFileIdOrderByCreatedAtAsc(file.getId()).stream()
                .map(FileMapper::toAttachment)
                .toList();
    }

    @Transactional
    public FileAttachmentResponse upload(
            FileEntity file, MultipartFile multipart, boolean responseDocument, SecurityUser actor) {
        assertUploadAllowed(file, responseDocument);
        validateFile(multipart);

        User uploader = userRepository.findById(actor.getId())
                .orElseThrow(() -> FileException.notFound("User not found"));

        String storageKey;
        try {
            storageKey = storageService.store(
                    file.getOrganization(),
                    file.getId(),
                    multipart.getOriginalFilename(),
                    multipart.getInputStream());
        } catch (IOException e) {
            throw FileException.badRequest("FILE_ATTACHMENT_INVALID", "Failed to store attachment: " + e.getMessage());
        }

        FileAttachment attachment = new FileAttachment();
        attachment.setFile(file);
        attachment.setOriginalFilename(multipart.getOriginalFilename());
        attachment.setContentType(multipart.getContentType() != null ? multipart.getContentType() : "application/octet-stream");
        attachment.setSizeBytes(multipart.getSize());
        attachment.setStorageBucket(LocalAttachmentStorageService.BUCKET);
        attachment.setStorageKey(storageKey);
        attachment.setResponseDocument(responseDocument);
        attachment.setUploadedBy(uploader);

        return FileMapper.toAttachment(fileAttachmentRepository.save(attachment));
    }

    @Transactional
    public void delete(FileEntity file, UUID attachmentId) {
        if (file.getStatus() != FileStatus.DRAFT) {
            throw FileException.conflict("FILE_ATTACHMENT_LOCKED", "Attachments cannot be deleted after submission");
        }
        FileAttachment attachment = fileAttachmentRepository.findByIdAndFileId(attachmentId, file.getId())
                .orElseThrow(() -> FileException.notFound("Attachment not found"));
        try {
            storageService.delete(attachment.getStorageKey());
        } catch (IOException e) {
            throw FileException.badRequest("FILE_ATTACHMENT_INVALID", "Failed to delete attachment file");
        }
        fileAttachmentRepository.delete(attachment);
    }

    @Transactional(readOnly = true)
    public Resource download(FileEntity file, UUID attachmentId) {
        FileAttachment attachment = fileAttachmentRepository.findByIdAndFileId(attachmentId, file.getId())
                .orElseThrow(() -> FileException.notFound("Attachment not found"));
        try {
            return storageService.loadAsResource(attachment.getStorageKey());
        } catch (IOException e) {
            throw FileException.notFound("Attachment file not found on storage");
        }
    }

    @Transactional(readOnly = true)
    public FileAttachment getAttachment(FileEntity file, UUID attachmentId) {
        return fileAttachmentRepository.findByIdAndFileId(attachmentId, file.getId())
                .orElseThrow(() -> FileException.notFound("Attachment not found"));
    }

    @Transactional(readOnly = true)
    public String getOriginalFilename(FileEntity file, UUID attachmentId) {
        return getAttachment(file, attachmentId).getOriginalFilename();
    }

    @Transactional(readOnly = true)
    public String getContentType(FileEntity file, UUID attachmentId) {
        return getAttachment(file, attachmentId).getContentType();
    }

    void validateFile(MultipartFile multipart) {
        if (multipart == null || multipart.isEmpty()) {
            throw FileException.badRequest("FILE_ATTACHMENT_INVALID", "Attachment file is required");
        }
        if (multipart.getSize() > MAX_SIZE_BYTES) {
            throw FileException.badRequest("FILE_ATTACHMENT_INVALID", "Attachment exceeds maximum size of 20 MB");
        }
        String contentType = multipart.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw FileException.badRequest(
                    "FILE_ATTACHMENT_INVALID",
                    "Attachment type not allowed. Allowed: PDF, DOCX, XLSX, JPEG, PNG");
        }
    }

    private void assertUploadAllowed(FileEntity file, boolean responseDocument) {
        if (file.getStatus() == FileStatus.DRAFT) {
            return;
        }
        if (file.getStatus() == FileStatus.IN_PROGRESS && responseDocument) {
            return;
        }
        throw FileException.conflict("FILE_ATTACHMENT_LOCKED", "Attachments can only be uploaded on draft files");
    }
}
