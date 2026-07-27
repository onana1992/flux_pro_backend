-- Objectif : appliquer le profil d'alertes par défaut (CDC §10.2 / AlertRuleSeedProfileService)
--            à tous les templates de chaîne qui n'ont encore aucune règle.
-- Tables : alert_rules
-- Prérequis : alert_types seedés (REMINDER, OVERDUE, ESCALATION)
-- Idempotent : n'insère que pour les templates sans aucune règle, et saute les doublons logiques
-- Profil (jours ouvrés) :
--   1. J_MINUS_2  REMINDER   → CURRENT_RESPONSIBLE
--   2. J_PLUS_0   OVERDUE    → CURRENT_RESPONSIBLE
--   3. J_PLUS_0   OVERDUE    → ROLE SERVICE_HEAD
--   4. J_PLUS_3   ESCALATION → ROLE DIRECTOR (L1)
--   5. J_PLUS_7   ESCALATION → ROLE SECRETARY_GENERAL (L2)
--   6. J_PLUS_15  ESCALATION → ROLE EXECUTIVE_OFFICE (L3, URGENT_PLUS)

SET NAMES utf8mb4;

SET @type_reminder   := (SELECT id FROM alert_types WHERE code = 'REMINDER'   LIMIT 1);
SET @type_overdue    := (SELECT id FROM alert_types WHERE code = 'OVERDUE'    LIMIT 1);
SET @type_escalation := (SELECT id FROM alert_types WHERE code = 'ESCALATION' LIMIT 1);

DROP TEMPORARY TABLE IF EXISTS tmp_templates_without_rules;
CREATE TEMPORARY TABLE tmp_templates_without_rules AS
SELECT ct.id AS chain_template_id, ct.code AS template_code
FROM chain_templates ct
WHERE NOT EXISTS (
    SELECT 1 FROM alert_rules ar WHERE ar.chain_template_id = ct.id
);

DROP TEMPORARY TABLE IF EXISTS tmp_alert_profile;
CREATE TEMPORARY TABLE tmp_alert_profile (
    sort_order INT NOT NULL,
    threshold_code VARCHAR(20) NOT NULL,
    offset_value INT NOT NULL,
    offset_unit VARCHAR(20) NOT NULL,
    alert_type_id BINARY(16) NOT NULL,
    escalation_level INT NULL,
    target_mode VARCHAR(20) NOT NULL,
    target_role VARCHAR(30) NULL,
    priority_scope VARCHAR(20) NULL
);

INSERT INTO tmp_alert_profile VALUES
(1, 'J_MINUS_2', -2, 'WORKING_DAYS', @type_reminder, NULL, 'CURRENT_RESPONSIBLE', NULL, NULL),
(2, 'J_PLUS_0',   0, 'WORKING_DAYS', @type_overdue,    NULL, 'CURRENT_RESPONSIBLE', NULL, NULL),
(3, 'J_PLUS_0',   0, 'WORKING_DAYS', @type_overdue,    NULL, 'ROLE', 'SERVICE_HEAD', NULL),
(4, 'J_PLUS_3',   3, 'WORKING_DAYS', @type_escalation, 1,    'ROLE', 'DIRECTOR', NULL),
(5, 'J_PLUS_7',   7, 'WORKING_DAYS', @type_escalation, 2,    'ROLE', 'SECRETARY_GENERAL', NULL),
(6, 'J_PLUS_15', 15, 'WORKING_DAYS', @type_escalation, 3,    'ROLE', 'EXECUTIVE_OFFICE', 'URGENT_PLUS');

SELECT COUNT(*) AS templates_to_seed FROM tmp_templates_without_rules;

INSERT INTO alert_rules (
    id,
    chain_template_id,
    chain_step_template_id,
    threshold_code,
    offset_value,
    offset_unit,
    alert_type_id,
    escalation_level,
    target_mode,
    target_role,
    priority_scope,
    active,
    created_at,
    updated_at
)
SELECT
    UNHEX(REPLACE(UUID(), '-', '')),
    t.chain_template_id,
    NULL,
    p.threshold_code,
    p.offset_value,
    p.offset_unit,
    p.alert_type_id,
    p.escalation_level,
    p.target_mode,
    p.target_role,
    p.priority_scope,
    TRUE,
    NOW(6),
    NOW(6)
FROM tmp_templates_without_rules t
CROSS JOIN tmp_alert_profile p
WHERE p.alert_type_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM alert_rules ar
      WHERE ar.chain_template_id = t.chain_template_id
        AND ar.threshold_code = p.threshold_code
        AND ar.target_mode = p.target_mode
        AND (
              (ar.target_role IS NULL AND p.target_role IS NULL)
              OR ar.target_role = p.target_role
          )
  );

SELECT
    ct.code,
    COUNT(ar.id) AS rules_count
FROM chain_templates ct
LEFT JOIN alert_rules ar ON ar.chain_template_id = ct.id
GROUP BY ct.id, ct.code
ORDER BY rules_count ASC, ct.code;

SELECT COUNT(*) AS templates_still_without_rules
FROM chain_templates ct
WHERE NOT EXISTS (
    SELECT 1 FROM alert_rules ar WHERE ar.chain_template_id = ct.id
);
