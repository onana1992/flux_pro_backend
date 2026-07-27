package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.PreconfiguredDossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PreconfiguredDossierRepository extends JpaRepository<PreconfiguredDossier, UUID> {

    Optional<PreconfiguredDossier> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByFileTypeCodeIgnoreCase(String fileTypeCode);

    boolean existsByChainTemplateId(UUID chainTemplateId);

    @Query("""
            SELECT DISTINCT d FROM PreconfiguredDossier d
            LEFT JOIN FETCH d.chainTemplate ct
            LEFT JOIN FETCH ct.steps
            ORDER BY d.sortOrder ASC
            """)
    List<PreconfiguredDossier> findAllWithChainOrderBySortOrderAsc();

    @Query("""
            SELECT DISTINCT d FROM PreconfiguredDossier d
            LEFT JOIN FETCH d.chainTemplate ct
            LEFT JOIN FETCH ct.steps
            WHERE d.active = true AND d.portalEnabled = true
            ORDER BY d.sortOrder ASC
            """)
    List<PreconfiguredDossier> findPortalEnabled();

    @Query("""
            SELECT DISTINCT d FROM PreconfiguredDossier d
            LEFT JOIN FETCH d.chainTemplate ct
            LEFT JOIN FETCH ct.steps
            WHERE LOWER(d.code) = LOWER(:code)
            """)
    Optional<PreconfiguredDossier> findByCodeIgnoreCaseWithChain(@Param("code") String code);

    @Query("""
            SELECT DISTINCT d FROM PreconfiguredDossier d
            LEFT JOIN FETCH d.chainTemplate ct
            LEFT JOIN FETCH ct.steps
            WHERE d.id = :id
            """)
    Optional<PreconfiguredDossier> findByIdWithChain(@Param("id") UUID id);
}
