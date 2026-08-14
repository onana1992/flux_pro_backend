package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileAttachmentRepository extends JpaRepository<FileAttachment, UUID> {

    @Query("""
            SELECT a FROM FileAttachment a
            LEFT JOIN FETCH a.passage p
            LEFT JOIN FETCH p.chainStepTemplate
            LEFT JOIN FETCH p.responsibleUser
            LEFT JOIN FETCH a.uploadedBy
            LEFT JOIN FETCH a.uploadedByPortalUser
            WHERE a.file.id = :fileId
            ORDER BY a.createdAt ASC
            """)
    List<FileAttachment> findByFileIdOrderByCreatedAtAsc(@Param("fileId") UUID fileId);

    @Query("""
            SELECT a FROM FileAttachment a
            LEFT JOIN FETCH a.passage p
            LEFT JOIN FETCH p.chainStepTemplate
            LEFT JOIN FETCH p.responsibleUser
            LEFT JOIN FETCH a.uploadedBy
            LEFT JOIN FETCH a.uploadedByPortalUser
            WHERE a.id = :id AND a.file.id = :fileId
            """)
    Optional<FileAttachment> findByIdAndFileId(@Param("id") UUID id, @Param("fileId") UUID fileId);

    long countByFileIdAndPortalAttachmentKey(UUID fileId, String portalAttachmentKey);
}
