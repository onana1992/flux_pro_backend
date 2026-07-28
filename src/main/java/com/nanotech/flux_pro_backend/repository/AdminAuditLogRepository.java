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

    /**
     * Filtres optionnels via flags booléens — évite {@code :param IS NULL} que PostgreSQL
     * ne type pas ({@code could not determine data type of parameter}).
     */
    @Query("""
            SELECT a FROM AdminAuditLog a
            WHERE (:resourceTypeEmpty = TRUE OR a.resourceType = :resourceType)
              AND (:actionEmpty = TRUE OR a.action = :action)
              AND (:actorEmailEmpty = TRUE
                   OR LOWER(a.actorEmail) LIKE LOWER(CONCAT('%', :actorEmail, '%')))
              AND (:hasSuccess = FALSE OR a.success = :success)
              AND (:hasFrom = FALSE OR a.createdAt >= :from)
              AND (:hasTo = FALSE OR a.createdAt <= :to)
            ORDER BY a.createdAt DESC
            """)
    Page<AdminAuditLog> search(
            @Param("resourceTypeEmpty") boolean resourceTypeEmpty,
            @Param("resourceType") String resourceType,
            @Param("actionEmpty") boolean actionEmpty,
            @Param("action") String action,
            @Param("actorEmailEmpty") boolean actorEmailEmpty,
            @Param("actorEmail") String actorEmail,
            @Param("hasSuccess") boolean hasSuccess,
            @Param("success") boolean success,
            @Param("hasFrom") boolean hasFrom,
            @Param("from") Instant from,
            @Param("hasTo") boolean hasTo,
            @Param("to") Instant to,
            Pageable pageable);
}
