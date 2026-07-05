package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.FilePassage;
import com.nanotech.flux_pro_backend.enumeration.PassageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    /**
     * Widget « Top retards » (DSH-01/02/03) et compteur « en retard » (DSH-05) : même définition
     * du retard que {@link #findOverdueForDigest}, restreinte au périmètre organisationnel de l'appelant.
     */
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
              AND (:allOrgs = true OR f.organization.id IN :orgIds)
              AND (:organizationId IS NULL OR f.organization.id = :organizationId)
            ORDER BY p.dueAt ASC
            """)
    List<FilePassage> findOverdueForScope(
            @Param("now") Instant now,
            @Param("allOrgs") boolean allOrgs,
            @Param("orgIds") Set<UUID> orgIds,
            @Param("organizationId") UUID organizationId);

    /** Charge par agent (DSH-02) : maillons actifs dont le responsable appartient au périmètre demandé. */
    @Query("""
            SELECT p FROM FilePassage p
            JOIN FETCH p.responsibleUser ru
            JOIN FETCH ru.organization
            JOIN FETCH p.file f
            WHERE p.status = com.nanotech.flux_pro_backend.enumeration.PassageStatus.IN_PROGRESS
              AND f.status = com.nanotech.flux_pro_backend.enumeration.FileStatus.IN_PROGRESS
              AND p.responsibleUser IS NOT NULL
              AND (:allOrgs = true OR ru.organization.id IN :orgIds)
              AND (:organizationId IS NULL OR ru.organization.id = :organizationId)
            """)
    List<FilePassage> findActiveForWorkload(
            @Param("allOrgs") boolean allOrgs,
            @Param("orgIds") Set<UUID> orgIds,
            @Param("organizationId") UUID organizationId);

    /** Widget « Mon activité » (DSH-01) : maillons actifs dont l'utilisateur courant est responsable. */
    @Query("""
            SELECT p FROM FilePassage p
            JOIN FETCH p.file f
            JOIN FETCH p.chainStepTemplate
            WHERE p.responsibleUser.id = :userId
              AND p.status = com.nanotech.flux_pro_backend.enumeration.PassageStatus.IN_PROGRESS
              AND f.status = com.nanotech.flux_pro_backend.enumeration.FileStatus.IN_PROGRESS
            """)
    List<FilePassage> findActiveByResponsibleUser(@Param("userId") UUID userId);

    /** Widget « Mon activité » (DSH-01) : mes transmissions récentes (maillons que j'ai quittés). */
    @Query("""
            SELECT p FROM FilePassage p
            JOIN FETCH p.file f
            JOIN FETCH p.chainStepTemplate
            WHERE p.responsibleUser.id = :userId
              AND p.status IN (
                  com.nanotech.flux_pro_backend.enumeration.PassageStatus.COMPLETED,
                  com.nanotech.flux_pro_backend.enumeration.PassageStatus.RETURNED)
              AND p.transmittedAt >= :since
            """)
    List<FilePassage> findRecentTransmissionsByResponsibleUser(
            @Param("userId") UUID userId, @Param("since") Instant since);
}
