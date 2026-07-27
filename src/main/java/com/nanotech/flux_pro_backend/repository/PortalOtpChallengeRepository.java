package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.PortalOtpChallenge;
import com.nanotech.flux_pro_backend.enumeration.PortalOtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PortalOtpChallengeRepository extends JpaRepository<PortalOtpChallenge, UUID> {

    Optional<PortalOtpChallenge> findFirstByEmailIgnoreCaseAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            String email, PortalOtpPurpose purpose);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE PortalOtpChallenge c
            SET c.consumedAt = :now
            WHERE LOWER(c.email) = LOWER(:email)
              AND c.purpose = :purpose
              AND c.consumedAt IS NULL
            """)
    int consumeAllOpen(
            @Param("email") String email,
            @Param("purpose") PortalOtpPurpose purpose,
            @Param("now") Instant now);
}
