package com.nanotech.flux_pro_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Catalogue administrable des types d'alerte (ALR-F17) — PAS une énumération Java.
 * REMINDER / OVERDUE / ESCALATION / DAILY_DIGEST sont de simples lignes de seed
 * (systemDefined = true) ; un admin métier peut créer d'autres types à tout moment,
 * sans redéploiement (cf. docs/SPEC-ALR.md §6.2).
 */
@Entity
@Table(name = "alert_types")
@Getter
@Setter
public class AlertType extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "email_template_code", length = 100)
    private String emailTemplateCode;

    @Column(name = "system_defined", nullable = false)
    private boolean systemDefined = false;

    @Column(nullable = false)
    private boolean active = true;
}
