package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.FilePassage;
import com.nanotech.flux_pro_backend.enumeration.PassageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FilePassageRepository extends JpaRepository<FilePassage, UUID> {

    boolean existsByFileId(UUID fileId);

    boolean existsByFileIdAndStatus(UUID fileId, PassageStatus status);

    @Query("""
            SELECT p FROM FilePassage p
            JOIN FETCH p.chainStepTemplate
            LEFT JOIN FETCH p.responsibleUser ru
            LEFT JOIN FETCH ru.organization
            WHERE p.file.id = :fileId
            ORDER BY p.stepOrder ASC
            """)
    List<FilePassage> findByFileIdWithDetails(@Param("fileId") UUID fileId);

    @Query("""
            SELECT p FROM FilePassage p
            JOIN FETCH p.chainStepTemplate
            LEFT JOIN FETCH p.responsibleUser ru
            LEFT JOIN FETCH ru.organization
            WHERE p.id = :id AND p.file.id = :fileId
            """)
    Optional<FilePassage> findByIdAndFileIdWithDetails(@Param("id") UUID id, @Param("fileId") UUID fileId);

    @Query("""
            SELECT p FROM FilePassage p
            JOIN FETCH p.chainStepTemplate
            LEFT JOIN FETCH p.responsibleUser ru
            LEFT JOIN FETCH ru.organization
            WHERE p.file.id = :fileId AND p.status = :status
            """)
    Optional<FilePassage> findByFileIdAndStatusWithDetails(
            @Param("fileId") UUID fileId, @Param("status") PassageStatus status);

    /**
     * Candidats du moteur d'alertes (ALR) : maillons actifs d'un dossier actif, avec échéance connue.
     * SUSPENDED est exclu par la condition p.status = 'IN_PROGRESS' (ALR-07).
     */
    @Query("""
            SELECT p FROM FilePassage p
            JOIN FETCH p.file f
            JOIN FETCH p.chainStepTemplate
            LEFT JOIN FETCH p.responsibleUser
            WHERE p.status = com.nanotech.flux_pro_backend.enumeration.PassageStatus.IN_PROGRESS
              AND f.status = com.nanotech.flux_pro_backend.enumeration.FileStatus.IN_PROGRESS
              AND p.dueAt IS NOT NULL
            """)
    List<FilePassage> findActiveCandidatesForAlerts();

    /** Maillons en retard (échéance dépassée) pour le digest quotidien (ALR-08). */
    @Query("""
            SELECT p FROM FilePassage p
            JOIN FETCH p.file f
            JOIN FETCH f.organization
            JOIN FETCH p.chainStepTemplate
            LEFT JOIN FETCH p.responsibleUser
            WHERE p.status = com.nanotech.flux_pro_backend.enumeration.PassageStatus.IN_PROGRESS
              AND f.status = com.nanotech.flux_pro_backend.enumeration.FileStatus.IN_PROGRESS
              AND p.dueAt IS NOT NULL
              AND p.dueAt < :now
            """)
    List<FilePassage> findOverdueForDigest(@Param("now") Instant now);
}
