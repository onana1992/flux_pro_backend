-- Objectif : associer des règles d'alerte pertinentes aux templates T01–T06 (ALR-06 / CDC §10.2)
-- Tables impactées : alert_types (seed si absent), alert_rules (insert)
-- Prérequis :
--   - docs/sql/2026-07-04_alert_types.sql
--   - docs/sql/2026-07-04_alert_rules.sql
--   - templates T01–T06 présents (ChainDataInitializer + évent. 2026-07-08_seed_chain_template_parallel.sql
--     ou 2026-07-11_reset_seed_mintp_files_chains.sql)
-- Exécution : manuelle sur MySQL / MariaDB (ddl-auto=none)
-- Idempotent : ne crée que les règles absentes (clé logique template + threshold + mode + rôle)
--
-- Profils :
--   T01, T03, T04, T05, T06 → matrice CDC §10.2 en jours ouvrés
--   T02 (circuit urgent / heures) → mêmes paliers adaptés en heures ouvrées
--
-- UUID fixes : a2000000-0000-4000-8000-00000000{T}{R}
--   T = 1..6 (template), R = 01..06 (ligne du profil)

-- ═══════════════════════════════════════════════════════════════════════════
-- 0. Types d'alerte seed (si absents — normalement créés par AlertTypeDataInitializer)
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO alert_types (id, code, label, description, email_template_code, system_defined, active, created_at, updated_at)
SELECT UNHEX(REPLACE('b1000000-0000-4000-8000-000000000001', '-', '')),
       'REMINDER', 'Rappel avant échéance',
       'Rappel envoyé au responsable actuel avant l''échéance du maillon (ALR-01)',
       'alert-reminder', TRUE, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM alert_types WHERE code = 'REMINDER');

INSERT INTO alert_types (id, code, label, description, email_template_code, system_defined, active, created_at, updated_at)
SELECT UNHEX(REPLACE('b1000000-0000-4000-8000-000000000002', '-', '')),
       'OVERDUE', 'Dépassement d''échéance',
       'Notification envoyée à l''échéance dépassée (ALR-02)',
       'alert-overdue', TRUE, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM alert_types WHERE code = 'OVERDUE');

INSERT INTO alert_types (id, code, label, description, email_template_code, system_defined, active, created_at, updated_at)
SELECT UNHEX(REPLACE('b1000000-0000-4000-8000-000000000003', '-', '')),
       'ESCALATION', 'Escalade hiérarchique',
       'Escalade envoyée au palier hiérarchique supérieur configuré (ALR-03/ALR-04)',
       'alert-escalation', TRUE, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM alert_types WHERE code = 'ESCALATION');

INSERT INTO alert_types (id, code, label, description, email_template_code, system_defined, active, created_at, updated_at)
SELECT UNHEX(REPLACE('b1000000-0000-4000-8000-000000000004', '-', '')),
       'DAILY_DIGEST', 'Récapitulatif quotidien des retards',
       'Digest quotidien des dossiers en retard (ALR-08)',
       'alert-daily-digest', TRUE, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM alert_types WHERE code = 'DAILY_DIGEST');

SET @type_reminder   := (SELECT id FROM alert_types WHERE code = 'REMINDER'   LIMIT 1);
SET @type_overdue    := (SELECT id FROM alert_types WHERE code = 'OVERDUE'    LIMIT 1);
SET @type_escalation := (SELECT id FROM alert_types WHERE code = 'ESCALATION' LIMIT 1);

SET @t01 := (SELECT id FROM chain_templates WHERE code = 'T01' LIMIT 1);
SET @t02 := (SELECT id FROM chain_templates WHERE code = 'T02' LIMIT 1);
SET @t03 := (SELECT id FROM chain_templates WHERE code = 'T03' LIMIT 1);
SET @t04 := (SELECT id FROM chain_templates WHERE code = 'T04' LIMIT 1);
SET @t05 := (SELECT id FROM chain_templates WHERE code = 'T05' LIMIT 1);
SET @t06 := (SELECT id FROM chain_templates WHERE code = 'T06' LIMIT 1);

-- ═══════════════════════════════════════════════════════════════════════════
-- 1. Profil CDC §10.2 (jours ouvrés) — T01, T03, T04, T05, T06
--    J-2 rappel | J+0 retard (resp. + chef) | J+3 Dir | J+7 SG | J+15 Cabinet (urgent+)
-- ═══════════════════════════════════════════════════════════════════════════

-- --- T01 Courrier entrant standard ------------------------------------------------
INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000101', '-', '')), @t01, NULL,
       'J_MINUS_2', -2, 'WORKING_DAYS', @type_reminder, NULL,
       'CURRENT_RESPONSIBLE', NULL, NULL, TRUE, NOW(6), NOW(6)
WHERE @t01 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t01
        AND ar.threshold_code = 'J_MINUS_2'
        AND ar.target_mode = 'CURRENT_RESPONSIBLE'
        AND ar.target_role IS NULL);

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000102', '-', '')), @t01, NULL,
       'J_PLUS_0', 0, 'WORKING_DAYS', @type_overdue, NULL,
       'CURRENT_RESPONSIBLE', NULL, NULL, TRUE, NOW(6), NOW(6)
WHERE @t01 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t01
        AND ar.threshold_code = 'J_PLUS_0'
        AND ar.target_mode = 'CURRENT_RESPONSIBLE'
        AND ar.target_role IS NULL);

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000103', '-', '')), @t01, NULL,
       'J_PLUS_0', 0, 'WORKING_DAYS', @type_overdue, NULL,
       'ROLE', 'SERVICE_HEAD', NULL, TRUE, NOW(6), NOW(6)
WHERE @t01 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t01
        AND ar.threshold_code = 'J_PLUS_0'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'SERVICE_HEAD');

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000104', '-', '')), @t01, NULL,
       'J_PLUS_3', 3, 'WORKING_DAYS', @type_escalation, 1,
       'ROLE', 'DIRECTOR', NULL, TRUE, NOW(6), NOW(6)
WHERE @t01 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t01
        AND ar.threshold_code = 'J_PLUS_3'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'DIRECTOR');

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000105', '-', '')), @t01, NULL,
       'J_PLUS_7', 7, 'WORKING_DAYS', @type_escalation, 2,
       'ROLE', 'SECRETARY_GENERAL', NULL, TRUE, NOW(6), NOW(6)
WHERE @t01 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t01
        AND ar.threshold_code = 'J_PLUS_7'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'SECRETARY_GENERAL');

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000106', '-', '')), @t01, NULL,
       'J_PLUS_15', 15, 'WORKING_DAYS', @type_escalation, 3,
       'ROLE', 'EXECUTIVE_OFFICE', 'URGENT_PLUS', TRUE, NOW(6), NOW(6)
WHERE @t01 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t01
        AND ar.threshold_code = 'J_PLUS_15'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'EXECUTIVE_OFFICE');

-- --- T03 Marché public simplifié --------------------------------------------------
INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000301', '-', '')), @t03, NULL,
       'J_MINUS_2', -2, 'WORKING_DAYS', @type_reminder, NULL,
       'CURRENT_RESPONSIBLE', NULL, NULL, TRUE, NOW(6), NOW(6)
WHERE @t03 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t03
        AND ar.threshold_code = 'J_MINUS_2'
        AND ar.target_mode = 'CURRENT_RESPONSIBLE'
        AND ar.target_role IS NULL);

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000302', '-', '')), @t03, NULL,
       'J_PLUS_0', 0, 'WORKING_DAYS', @type_overdue, NULL,
       'CURRENT_RESPONSIBLE', NULL, NULL, TRUE, NOW(6), NOW(6)
WHERE @t03 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t03
        AND ar.threshold_code = 'J_PLUS_0'
        AND ar.target_mode = 'CURRENT_RESPONSIBLE'
        AND ar.target_role IS NULL);

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000303', '-', '')), @t03, NULL,
       'J_PLUS_0', 0, 'WORKING_DAYS', @type_overdue, NULL,
       'ROLE', 'SERVICE_HEAD', NULL, TRUE, NOW(6), NOW(6)
WHERE @t03 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t03
        AND ar.threshold_code = 'J_PLUS_0'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'SERVICE_HEAD');

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000304', '-', '')), @t03, NULL,
       'J_PLUS_3', 3, 'WORKING_DAYS', @type_escalation, 1,
       'ROLE', 'DIRECTOR', NULL, TRUE, NOW(6), NOW(6)
WHERE @t03 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t03
        AND ar.threshold_code = 'J_PLUS_3'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'DIRECTOR');

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000305', '-', '')), @t03, NULL,
       'J_PLUS_7', 7, 'WORKING_DAYS', @type_escalation, 2,
       'ROLE', 'SECRETARY_GENERAL', NULL, TRUE, NOW(6), NOW(6)
WHERE @t03 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t03
        AND ar.threshold_code = 'J_PLUS_7'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'SECRETARY_GENERAL');

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000306', '-', '')), @t03, NULL,
       'J_PLUS_15', 15, 'WORKING_DAYS', @type_escalation, 3,
       'ROLE', 'EXECUTIVE_OFFICE', 'URGENT_PLUS', TRUE, NOW(6), NOW(6)
WHERE @t03 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t03
        AND ar.threshold_code = 'J_PLUS_15'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'EXECUTIVE_OFFICE');

-- --- T04 Autorisation travaux DRTP ------------------------------------------------
INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000401', '-', '')), @t04, NULL,
       'J_MINUS_2', -2, 'WORKING_DAYS', @type_reminder, NULL,
       'CURRENT_RESPONSIBLE', NULL, NULL, TRUE, NOW(6), NOW(6)
WHERE @t04 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t04
        AND ar.threshold_code = 'J_MINUS_2'
        AND ar.target_mode = 'CURRENT_RESPONSIBLE'
        AND ar.target_role IS NULL);

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000402', '-', '')), @t04, NULL,
       'J_PLUS_0', 0, 'WORKING_DAYS', @type_overdue, NULL,
       'CURRENT_RESPONSIBLE', NULL, NULL, TRUE, NOW(6), NOW(6)
WHERE @t04 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t04
        AND ar.threshold_code = 'J_PLUS_0'
        AND ar.target_mode = 'CURRENT_RESPONSIBLE'
        AND ar.target_role IS NULL);

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000403', '-', '')), @t04, NULL,
       'J_PLUS_0', 0, 'WORKING_DAYS', @type_overdue, NULL,
       'ROLE', 'SERVICE_HEAD', NULL, TRUE, NOW(6), NOW(6)
WHERE @t04 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t04
        AND ar.threshold_code = 'J_PLUS_0'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'SERVICE_HEAD');

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000404', '-', '')), @t04, NULL,
       'J_PLUS_3', 3, 'WORKING_DAYS', @type_escalation, 1,
       'ROLE', 'DIRECTOR', NULL, TRUE, NOW(6), NOW(6)
WHERE @t04 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t04
        AND ar.threshold_code = 'J_PLUS_3'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'DIRECTOR');

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000405', '-', '')), @t04, NULL,
       'J_PLUS_7', 7, 'WORKING_DAYS', @type_escalation, 2,
       'ROLE', 'SECRETARY_GENERAL', NULL, TRUE, NOW(6), NOW(6)
WHERE @t04 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t04
        AND ar.threshold_code = 'J_PLUS_7'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'SECRETARY_GENERAL');

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000406', '-', '')), @t04, NULL,
       'J_PLUS_15', 15, 'WORKING_DAYS', @type_escalation, 3,
       'ROLE', 'EXECUTIVE_OFFICE', 'URGENT_PLUS', TRUE, NOW(6), NOW(6)
WHERE @t04 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t04
        AND ar.threshold_code = 'J_PLUS_15'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'EXECUTIVE_OFFICE');

-- --- T05 Coopération / partenariat (template inactif mais règles prêtes) ----------
INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000501', '-', '')), @t05, NULL,
       'J_MINUS_2', -2, 'WORKING_DAYS', @type_reminder, NULL,
       'CURRENT_RESPONSIBLE', NULL, NULL, TRUE, NOW(6), NOW(6)
WHERE @t05 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t05
        AND ar.threshold_code = 'J_MINUS_2'
        AND ar.target_mode = 'CURRENT_RESPONSIBLE'
        AND ar.target_role IS NULL);

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000502', '-', '')), @t05, NULL,
       'J_PLUS_0', 0, 'WORKING_DAYS', @type_overdue, NULL,
       'CURRENT_RESPONSIBLE', NULL, NULL, TRUE, NOW(6), NOW(6)
WHERE @t05 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t05
        AND ar.threshold_code = 'J_PLUS_0'
        AND ar.target_mode = 'CURRENT_RESPONSIBLE'
        AND ar.target_role IS NULL);

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000503', '-', '')), @t05, NULL,
       'J_PLUS_0', 0, 'WORKING_DAYS', @type_overdue, NULL,
       'ROLE', 'SERVICE_HEAD', NULL, TRUE, NOW(6), NOW(6)
WHERE @t05 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t05
        AND ar.threshold_code = 'J_PLUS_0'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'SERVICE_HEAD');

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000504', '-', '')), @t05, NULL,
       'J_PLUS_3', 3, 'WORKING_DAYS', @type_escalation, 1,
       'ROLE', 'DIRECTOR', NULL, TRUE, NOW(6), NOW(6)
WHERE @t05 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t05
        AND ar.threshold_code = 'J_PLUS_3'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'DIRECTOR');

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000505', '-', '')), @t05, NULL,
       'J_PLUS_7', 7, 'WORKING_DAYS', @type_escalation, 2,
       'ROLE', 'SECRETARY_GENERAL', NULL, TRUE, NOW(6), NOW(6)
WHERE @t05 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t05
        AND ar.threshold_code = 'J_PLUS_7'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'SECRETARY_GENERAL');

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000506', '-', '')), @t05, NULL,
       'J_PLUS_15', 15, 'WORKING_DAYS', @type_escalation, 3,
       'ROLE', 'EXECUTIVE_OFFICE', 'URGENT_PLUS', TRUE, NOW(6), NOW(6)
WHERE @t05 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t05
        AND ar.threshold_code = 'J_PLUS_15'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'EXECUTIVE_OFFICE');

-- --- T06 Marché — visas parallèles ------------------------------------------------
INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000601', '-', '')), @t06, NULL,
       'J_MINUS_2', -2, 'WORKING_DAYS', @type_reminder, NULL,
       'CURRENT_RESPONSIBLE', NULL, NULL, TRUE, NOW(6), NOW(6)
WHERE @t06 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t06
        AND ar.threshold_code = 'J_MINUS_2'
        AND ar.target_mode = 'CURRENT_RESPONSIBLE'
        AND ar.target_role IS NULL);

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000602', '-', '')), @t06, NULL,
       'J_PLUS_0', 0, 'WORKING_DAYS', @type_overdue, NULL,
       'CURRENT_RESPONSIBLE', NULL, NULL, TRUE, NOW(6), NOW(6)
WHERE @t06 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t06
        AND ar.threshold_code = 'J_PLUS_0'
        AND ar.target_mode = 'CURRENT_RESPONSIBLE'
        AND ar.target_role IS NULL);

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000603', '-', '')), @t06, NULL,
       'J_PLUS_0', 0, 'WORKING_DAYS', @type_overdue, NULL,
       'ROLE', 'SERVICE_HEAD', NULL, TRUE, NOW(6), NOW(6)
WHERE @t06 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t06
        AND ar.threshold_code = 'J_PLUS_0'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'SERVICE_HEAD');

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000604', '-', '')), @t06, NULL,
       'J_PLUS_3', 3, 'WORKING_DAYS', @type_escalation, 1,
       'ROLE', 'DIRECTOR', NULL, TRUE, NOW(6), NOW(6)
WHERE @t06 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t06
        AND ar.threshold_code = 'J_PLUS_3'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'DIRECTOR');

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000605', '-', '')), @t06, NULL,
       'J_PLUS_7', 7, 'WORKING_DAYS', @type_escalation, 2,
       'ROLE', 'SECRETARY_GENERAL', NULL, TRUE, NOW(6), NOW(6)
WHERE @t06 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t06
        AND ar.threshold_code = 'J_PLUS_7'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'SECRETARY_GENERAL');

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000606', '-', '')), @t06, NULL,
       'J_PLUS_15', 15, 'WORKING_DAYS', @type_escalation, 3,
       'ROLE', 'EXECUTIVE_OFFICE', 'URGENT_PLUS', TRUE, NOW(6), NOW(6)
WHERE @t06 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t06
        AND ar.threshold_code = 'J_PLUS_15'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'EXECUTIVE_OFFICE');

-- ═══════════════════════════════════════════════════════════════════════════
-- 2. Profil adapté T02 — Courrier très urgent (heures ouvrées)
--    H-1 rappel | H+0 retard | H+4 Dir | H+8 SG | H+16 Cabinet (urgent+)
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000201', '-', '')), @t02, NULL,
       'H_MINUS_1', -1, 'WORKING_HOURS', @type_reminder, NULL,
       'CURRENT_RESPONSIBLE', NULL, NULL, TRUE, NOW(6), NOW(6)
WHERE @t02 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t02
        AND ar.threshold_code = 'H_MINUS_1'
        AND ar.target_mode = 'CURRENT_RESPONSIBLE'
        AND ar.target_role IS NULL);

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000202', '-', '')), @t02, NULL,
       'H_PLUS_0', 0, 'WORKING_HOURS', @type_overdue, NULL,
       'CURRENT_RESPONSIBLE', NULL, NULL, TRUE, NOW(6), NOW(6)
WHERE @t02 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t02
        AND ar.threshold_code = 'H_PLUS_0'
        AND ar.target_mode = 'CURRENT_RESPONSIBLE'
        AND ar.target_role IS NULL);

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000203', '-', '')), @t02, NULL,
       'H_PLUS_0', 0, 'WORKING_HOURS', @type_overdue, NULL,
       'ROLE', 'SERVICE_HEAD', NULL, TRUE, NOW(6), NOW(6)
WHERE @t02 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t02
        AND ar.threshold_code = 'H_PLUS_0'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'SERVICE_HEAD');

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000204', '-', '')), @t02, NULL,
       'H_PLUS_4', 4, 'WORKING_HOURS', @type_escalation, 1,
       'ROLE', 'DIRECTOR', NULL, TRUE, NOW(6), NOW(6)
WHERE @t02 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t02
        AND ar.threshold_code = 'H_PLUS_4'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'DIRECTOR');

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000205', '-', '')), @t02, NULL,
       'H_PLUS_8', 8, 'WORKING_HOURS', @type_escalation, 2,
       'ROLE', 'SECRETARY_GENERAL', NULL, TRUE, NOW(6), NOW(6)
WHERE @t02 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t02
        AND ar.threshold_code = 'H_PLUS_8'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'SECRETARY_GENERAL');

INSERT INTO alert_rules (
    id, chain_template_id, chain_step_template_id,
    threshold_code, offset_value, offset_unit, alert_type_id, escalation_level,
    target_mode, target_role, priority_scope, active, created_at, updated_at
)
SELECT UNHEX(REPLACE('a2000000-0000-4000-8000-000000000206', '-', '')), @t02, NULL,
       'H_PLUS_16', 16, 'WORKING_HOURS', @type_escalation, 3,
       'ROLE', 'EXECUTIVE_OFFICE', 'URGENT_PLUS', TRUE, NOW(6), NOW(6)
WHERE @t02 IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM alert_rules ar
      WHERE ar.chain_template_id = @t02
        AND ar.threshold_code = 'H_PLUS_16'
        AND ar.target_mode = 'ROLE'
        AND ar.target_role = 'EXECUTIVE_OFFICE');

-- ═══════════════════════════════════════════════════════════════════════════
-- Vérification
-- ═══════════════════════════════════════════════════════════════════════════
-- SELECT ct.code, ar.threshold_code, ar.offset_value, ar.offset_unit,
--        at.code AS alert_type, ar.escalation_level, ar.target_mode, ar.target_role, ar.priority_scope
-- FROM alert_rules ar
-- JOIN chain_templates ct ON ct.id = ar.chain_template_id
-- JOIN alert_types at ON at.id = ar.alert_type_id
-- WHERE ct.code IN ('T01','T02','T03','T04','T05','T06')
-- ORDER BY ct.code, ar.offset_value, ar.target_mode, ar.target_role;

-- rollback (commenté) :
-- DELETE FROM alert_rules WHERE id IN (
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000101', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000102', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000103', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000104', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000105', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000106', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000201', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000202', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000203', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000204', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000205', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000206', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000301', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000302', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000303', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000304', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000305', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000306', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000401', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000402', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000403', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000404', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000405', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000406', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000501', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000502', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000503', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000504', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000505', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000506', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000601', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000602', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000603', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000604', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000605', '-', '')),
--   UNHEX(REPLACE('a2000000-0000-4000-8000-000000000606', '-', ''))
-- );
