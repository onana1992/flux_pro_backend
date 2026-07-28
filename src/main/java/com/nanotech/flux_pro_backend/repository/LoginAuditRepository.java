package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.LoginAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, UUID> {

    /**
     * Filtres optionnels via flags booléens — évite {@code :param IS NULL} que PostgreSQL
     * ne type pas ({@code could not determine data type of parameter}).
     */
    @Query("""
            SELECT la FROM LoginAudit la
            WHERE (:emailEmpty = TRUE OR LOWER(la.email) LIKE LOWER(CONCAT('%', :email, '%')))
              AND (:hasSuccess = FALSE OR la.success = :success)
              AND (:hasFrom = FALSE OR la.createdAt >= :from)
              AND (:hasTo = FALSE OR la.createdAt <= :to)
            ORDER BY la.createdAt DESC
            """)
    Page<LoginAudit> search(
            @Param("emailEmpty") boolean emailEmpty,
            @Param("email") String email,
            @Param("hasSuccess") boolean hasSuccess,
            @Param("success") boolean success,
            @Param("hasFrom") boolean hasFrom,
            @Param("from") Instant from,
            @Param("hasTo") boolean hasTo,
            @Param("to") Instant to,
            Pageable pageable);
}
