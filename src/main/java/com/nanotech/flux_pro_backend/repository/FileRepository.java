package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.FileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface FileRepository extends JpaRepository<FileEntity, UUID> {

    Optional<FileEntity> findByReferenceNumberIgnoreCase(String referenceNumber);

    boolean existsByReferenceNumberIgnoreCase(String referenceNumber);

    @Query("""
            SELECT DISTINCT f FROM FileEntity f
            LEFT JOIN FETCH f.organization
            LEFT JOIN FETCH f.chainTemplate
            WHERE f.id = :id
            """)
    Optional<FileEntity> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
            SELECT f FROM FileEntity f
            WHERE (:allOrgs = true OR f.organization.id IN :orgIds)
              AND (:organizationId IS NULL OR f.organization.id = :organizationId)
              AND (:fileTypeCode IS NULL OR f.fileTypeCode = :fileTypeCode)
              AND (:status IS NULL OR f.status = :status)
              AND (:priority IS NULL OR f.priority = :priority)
              AND (:receivedFrom IS NULL OR f.receivedAt >= :receivedFrom)
              AND (:receivedTo IS NULL OR f.receivedAt <= :receivedTo)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(f.referenceNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(f.subject) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(f.senderOrBeneficiary) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<FileEntity> search(
            @Param("allOrgs") boolean allOrgs,
            @Param("orgIds") Set<UUID> orgIds,
            @Param("organizationId") UUID organizationId,
            @Param("fileTypeCode") String fileTypeCode,
            @Param("status") com.nanotech.flux_pro_backend.enumeration.FileStatus status,
            @Param("priority") com.nanotech.flux_pro_backend.enumeration.FilePriority priority,
            @Param("receivedFrom") LocalDate receivedFrom,
            @Param("receivedTo") LocalDate receivedTo,
            @Param("search") String search,
            Pageable pageable);
}
