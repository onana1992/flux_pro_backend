package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {

    @Query("""
            SELECT a FROM AdminAuditLog a
            WHERE (:resourceType IS NULL OR :resourceType = '' OR a.resourceType = :resourceType)
              AND (:action IS NULL OR :action = '' OR a.action = :action)
              AND (:actorEmail IS NULL OR :actorEmail = '' OR LOWER(a.actorEmail) LIKE LOWER(CONCAT('%', :actorEmail, '%')))
              AND (:success IS NULL OR a.success = :success)
              AND (:from IS NULL OR a.createdAt >= :from)
              AND (:to IS NULL OR a.createdAt <= :to)
            ORDER BY a.createdAt DESC
            """)
    Page<AdminAuditLog> search(
            @Param("resourceType") String resourceType,
            @Param("action") String action,
            @Param("actorEmail") String actorEmail,
            @Param("success") Boolean success,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
