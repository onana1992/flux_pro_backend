package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileAttachmentRepository extends JpaRepository<FileAttachment, UUID> {

    List<FileAttachment> findByFileIdOrderByCreatedAtAsc(UUID fileId);

    Optional<FileAttachment> findByIdAndFileId(UUID id, UUID fileId);
}
