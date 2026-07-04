package com.nanotech.flux_pro_backend.entity;

import com.nanotech.flux_pro_backend.enumeration.AlertTargetMode;
import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
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

/**
 * Règle d'alerte (ALR-06) — toujours rattachée à un template de chaîne précis, exactement
 * comme ChainStepTemplate appartient à un ChainTemplate. Aucune règle globale/implicite :
 * un template sans règle ne génère aucune alerte (cf. docs/SPEC-ALR.md §6.3).
 */
@Entity
@Table(name = "alert_rules")
@Getter
@Setter
public class AlertRule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chain_template_id", nullable = false)
    private ChainTemplate chainTemplate;

    /** Optionnel : restreint la règle à un maillon précis du template (NULL = tous les maillons). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chain_step_template_id")
    private ChainStepTemplate chainStepTemplate;

    @Column(name = "threshold_code", nullable = false, length = 20)
    private String thresholdCode;

    @Column(name = "offset_value", nullable = false)
    private int offsetValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "offset_unit", nullable = false, length = 20)
    private DelayUnit offsetUnit = DelayUnit.WORKING_DAYS;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_type_id", nullable = false)
    private AlertType alertType;

    /** Simple numéro de palier (1, 2, 3…), sans signification métier propre. */
    @Column(name = "escalation_level")
    private Integer escalationLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_mode", nullable = false, length = 20)
    private AlertTargetMode targetMode = AlertTargetMode.ROLE;

    /** Requis si targetMode = ROLE ; n'importe quelle valeur de UserRole, jamais fixée par le moteur. */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_role", length = 30)
    private UserRole targetRole;

    /** NULL = toutes priorités ; 'URGENT_PLUS' = Urgent / Très urgent uniquement. */
    @Column(name = "priority_scope", length = 20)
    private String priorityScope;

    @Column(nullable = false)
    private boolean active = true;
}
