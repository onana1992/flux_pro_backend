package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.ChainTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ChainTemplateRepository extends JpaRepository<ChainTemplate, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    Optional<ChainTemplate> findByCodeIgnoreCase(String code);

    @Query("""
            SELECT DISTINCT t FROM ChainTemplate t
            LEFT JOIN FETCH t.steps
            WHERE t.id = :id
            """)
    Optional<ChainTemplate> findByIdWithSteps(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT t FROM ChainTemplate t
            LEFT JOIN FETCH t.steps
            WHERE UPPER(t.code) = UPPER(:code)
            """)
    Optional<ChainTemplate> findByCodeWithSteps(@Param("code") String code);

    boolean existsByFileTypeCodeIgnoreCase(String fileTypeCode);

    Optional<ChainTemplate> findFirstByFileTypeCodeIgnoreCaseAndActiveTrue(String fileTypeCode);

    @Query("""
            SELECT t FROM ChainTemplate t
            WHERE (:active IS NULL OR t.active = :active)
              AND (:fileTypeCode IS NULL OR t.fileTypeCode = :fileTypeCode)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(t.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<ChainTemplate> search(
            @Param("active") Boolean active,
            @Param("fileTypeCode") String fileTypeCode,
            @Param("search") String search,
            Pageable pageable);
}
