package com.nanotech.flux_pro_backend.entity;

import com.nanotech.flux_pro_backend.enumeration.AlertChannel;
import com.nanotech.flux_pro_backend.enumeration.AlertStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Alerte effectivement générée/envoyée (historique + notifications in-app).
 * file/filePassage/alertRule sont nullables : un digest quotidien (ALR-08) agrège plusieurs
 * dossiers et n'est donc rattaché à aucun maillon ni dossier précis.
 */
@Entity
@Table(name = "alerts")
@Getter
@Setter
public class Alert extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private FileEntity file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_passage_id")
    private FilePassage filePassage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_rule_id")
    private AlertRule alertRule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_type_id", nullable = false)
    private AlertType alertType;

    @Column(name = "escalation_level")
    private Integer escalationLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AlertChannel channel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AlertStatus status = AlertStatus.PENDING;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;
}
