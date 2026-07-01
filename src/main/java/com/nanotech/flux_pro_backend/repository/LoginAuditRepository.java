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

    @Query("""
            SELECT la FROM LoginAudit la
            WHERE (:email IS NULL OR :email = '' OR LOWER(la.email) LIKE LOWER(CONCAT('%', :email, '%')))
              AND (:success IS NULL OR la.success = :success)
              AND (:from IS NULL OR la.createdAt >= :from)
              AND (:to IS NULL OR la.createdAt <= :to)
            ORDER BY la.createdAt DESC
            """)
    Page<LoginAudit> search(
            @Param("email") String email,
            @Param("success") Boolean success,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
