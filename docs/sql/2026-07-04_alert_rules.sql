-- Objectif : règles d'alerte paramétrables par template de chaîne (ALR-06)
-- Tables impactées : alert_rules (création)
-- Prérequis : chain_templates, chain_step_templates, alert_types
-- Exécution : manuelle sur MySQL avant déploiement (ddl-auto=none)
--
-- IMPORTANT : chain_template_id est NOT NULL — il n'existe aucune règle globale/implicite.
-- Un template sans ligne dans cette table ne génère aucune alerte (cf. docs/SPEC-ALR.md §7 ALR-R04).
-- Le profil de seed CDC §10.2 n'est PAS injecté ici : il est appliqué à la demande, par template,
-- via POST /api/admin/chain-templates/{id}/alert-rules/apply-default-profile (ALR-F18),
-- ou en masse via docs/sql/2026-07-14_seed_alert_rules_t01_t06.sql (T01–T06).

CREATE TABLE IF NOT EXISTS alert_rules (
    id                      BINARY(16)   NOT NULL PRIMARY KEY,
    chain_template_id       BINARY(16)   NOT NULL,
    chain_step_template_id  BINARY(16)   NULL,
    threshold_code          VARCHAR(20)  NOT NULL,
    offset_value            INT          NOT NULL DEFAULT 0,
    offset_unit             VARCHAR(20)  NOT NULL DEFAULT 'WORKING_DAYS',
    alert_type_id           BINARY(16)   NOT NULL,
    escalation_level        INT          NULL,
    target_mode             VARCHAR(20)  NOT NULL DEFAULT 'ROLE',
    target_role             VARCHAR(30)  NULL,
    priority_scope          VARCHAR(20)  NULL,
    active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_alert_rule_chain_template
        FOREIGN KEY (chain_template_id) REFERENCES chain_templates(id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_rule_chain_step_template
        FOREIGN KEY (chain_step_template_id) REFERENCES chain_step_templates(id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_rule_alert_type
        FOREIGN KEY (alert_type_id) REFERENCES alert_types(id),
    INDEX idx_alert_rules_chain_template (chain_template_id),
    INDEX idx_alert_rules_chain_step_template (chain_step_template_id),
    INDEX idx_alert_rules_alert_type (alert_type_id),
    INDEX idx_alert_rules_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- rollback (commenté) :
-- DROP TABLE IF EXISTS alert_rules;
