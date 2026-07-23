package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.Alert;
import com.nanotech.flux_pro_backend.enumeration.AlertChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    boolean existsByFilePassageIdAndAlertRuleIdAndChannel(UUID filePassageId, UUID alertRuleId, AlertChannel channel);

    boolean existsByAlertTypeId(UUID alertTypeId);

    @Query("""
            SELECT a FROM Alert a
            JOIN FETCH a.alertType
            LEFT JOIN FETCH a.file
            LEFT JOIN FETCH a.filePassage fp
            LEFT JOIN FETCH fp.chainStepTemplate
            WHERE a.recipient.id = :recipientId AND a.channel = :channel
            ORDER BY a.createdAt DESC
            """)
    Page<Alert> findByRecipientIdAndChannel(
            @Param("recipientId") UUID recipientId, @Param("channel") AlertChannel channel, Pageable pageable);

    @Query("""
            SELECT a FROM Alert a
            JOIN FETCH a.alertType
            LEFT JOIN FETCH a.file
            LEFT JOIN FETCH a.filePassage fp
            LEFT JOIN FETCH fp.chainStepTemplate
            WHERE a.recipient.id = :recipientId AND a.channel = :channel AND a.readAt IS NULL
            ORDER BY a.createdAt DESC
            """)
    Page<Alert> findByRecipientIdAndChannelAndUnread(
            @Param("recipientId") UUID recipientId, @Param("channel") AlertChannel channel, Pageable pageable);

    long countByRecipientIdAndChannelAndReadAtIsNull(UUID recipientId, AlertChannel channel);

    List<Alert> findByRecipientIdAndChannelAndReadAtIsNull(UUID recipientId, AlertChannel channel);

    @Query("""
            SELECT a FROM Alert a
            JOIN FETCH a.alertType
            LEFT JOIN FETCH a.filePassage fp
            LEFT JOIN FETCH fp.chainStepTemplate
            LEFT JOIN FETCH a.recipient
            WHERE a.file.id = :fileId
            ORDER BY a.createdAt DESC
            """)
    List<Alert> findByFileIdOrderByCreatedAtDesc(@Param("fileId") UUID fileId);

    /** Idempotence des notifs d'arrivée pour une même activation (receivedAt). */
    @Query("""
            SELECT COUNT(a) > 0 FROM Alert a
            WHERE a.filePassage.id = :passageId
              AND a.alertType.id = :alertTypeId
              AND a.recipient.id = :recipientId
              AND a.channel = :channel
              AND a.createdAt >= :since
            """)
    boolean existsArrivalNotification(
            @Param("passageId") UUID passageId,
            @Param("alertTypeId") UUID alertTypeId,
            @Param("recipientId") UUID recipientId,
            @Param("channel") AlertChannel channel,
            @Param("since") Instant since);
}
