package com.nanotech.flux_pro_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Journal d'audit générique des actions d'administration (création, modification, exécution)
 * réalisées via les endpoints protégés par {@link com.nanotech.flux_pro_backend.security.RequiresPermission}.
 * Distinct de {@link LoginAudit} qui ne trace que les connexions.
 */
@Entity
@Table(name = "admin_audit_log")
@Getter
@Setter
public class AdminAuditLog extends BaseEntity {

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "actor_email", nullable = false)
    private String actorEmail;

    @Column(name = "resource_type", nullable = false, length = 64)
    private String resourceType;

    @Column(name = "action", nullable = false, length = 32)
    private String action;

    @Column(name = "resource_id", length = 64)
    private String resourceId;

    @Column(name = "resource_label", length = 255)
    private String resourceLabel;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
}
