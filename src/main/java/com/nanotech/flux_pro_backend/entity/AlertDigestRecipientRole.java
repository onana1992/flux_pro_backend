package com.nanotech.flux_pro_backend.entity;

import com.nanotech.flux_pro_backend.enumeration.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Rôle destinataire du digest quotidien des retards (ALR-08).
 * Plusieurs lignes = plusieurs rôles notifiés ; configurable via l'UI Paramètres.
 */
@Entity
@Table(
        name = "alert_digest_recipient_role",
        uniqueConstraints = @UniqueConstraint(name = "uk_digest_recipient_role", columnNames = "role"))
@Getter
@Setter
public class AlertDigestRecipientRole extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private UserRole role;
}
