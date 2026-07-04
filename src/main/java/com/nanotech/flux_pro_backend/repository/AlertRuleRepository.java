package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.AlertRule;
import com.nanotech.flux_pro_backend.enumeration.AlertTargetMode;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {

    @Query("""
            SELECT r FROM AlertRule r
            JOIN FETCH r.alertType
            LEFT JOIN FETCH r.chainStepTemplate
            WHERE r.chainTemplate.id = :chainTemplateId
            ORDER BY r.thresholdCode ASC
            """)
    List<AlertRule> findByChainTemplateId(@Param("chainTemplateId") UUID chainTemplateId);

    @Query("""
            SELECT r FROM AlertRule r
            JOIN FETCH r.alertType
            LEFT JOIN FETCH r.chainStepTemplate
            WHERE r.chainTemplate.id = :chainTemplateId AND r.active = true
            """)
    List<AlertRule> findByChainTemplateIdAndActiveTrue(@Param("chainTemplateId") UUID chainTemplateId);

    @Query("""
            SELECT r FROM AlertRule r
            JOIN FETCH r.alertType
            LEFT JOIN FETCH r.chainStepTemplate
            WHERE r.id = :id AND r.chainTemplate.id = :chainTemplateId
            """)
    Optional<AlertRule> findByIdAndChainTemplateId(@Param("id") UUID id, @Param("chainTemplateId") UUID chainTemplateId);

    boolean existsByChainTemplateIdAndThresholdCodeAndTargetModeAndTargetRole(
            UUID chainTemplateId, String thresholdCode, AlertTargetMode targetMode, UserRole targetRole);

    boolean existsByAlertTypeId(UUID alertTypeId);

    void deleteAllByChainTemplateId(UUID chainTemplateId);
}
