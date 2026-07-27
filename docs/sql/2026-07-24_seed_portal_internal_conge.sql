-- Objectif : seed dossier préconfiguré « Demande de congé » + type RH-CONGE + chaîne T-CONGE
-- Tables impactées : file_types, chain_templates, chain_step_templates, preconfigured_dossiers
-- Prérequis :
--   - docs/sql/2026-07-24_preconfigured_dossiers.sql
-- Exécution : manuelle sur MySQL / MariaDB (ddl-auto=none)
-- Idempotent : ré-exécutable
--
-- Code préconfiguré : RH-CONGE
-- Audience           : INTERNAL
-- Formulaire         : dateDebut, dateFin, typeConge, motif (+ PJ justificatif optionnelle)
-- Chaîne             : T-CONGE (FK explicite sur preconfigured_dossiers)

SET @ft_id := UNHEX(REPLACE('e1000000-0000-4000-8000-000000000010', '-', ''));
SET @tpl_id := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000701', '-', ''));
SET @pd_id := UNHEX(REPLACE('f1000000-0000-4000-8000-000000000010', '-', ''));

SET @form_schema := '{
  "fields": [
    {
      "key": "dateDebut",
      "label": "Date de début",
      "type": "DATE",
      "required": true
    },
    {
      "key": "dateFin",
      "label": "Date de fin",
      "type": "DATE",
      "required": true
    },
    {
      "key": "typeConge",
      "label": "Type de congé",
      "type": "ENUM",
      "required": true,
      "options": ["ANNUEL", "MALADIE", "SANS_SOLDE"]
    },
    {
      "key": "motif",
      "label": "Motif",
      "type": "TEXT",
      "required": false,
      "maxLength": 500
    }
  ],
  "requiredAttachments": [
    { "key": "justificatif", "label": "Justificatif", "minCount": 0 }
  ]
}';

SET @required_attachment_keys := '[]';

-- ═══════════════════════════════════════════════════════════════════════════
-- 1. Type de dossier RH-CONGE (classification seule)
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO file_types (
    id, code, name, name_en, description, direction_code, sort_order, active,
    created_at, updated_at
)
SELECT
    @ft_id,
    'RH-CONGE',
    'Demande de congé',
    'Leave request',
    'Classification RH — demandes de congé.',
    'DAG',
    60,
    TRUE,
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'RH-CONGE');

SET @ft_id := (SELECT id FROM file_types WHERE code = 'RH-CONGE' LIMIT 1);

UPDATE file_types
SET
    name = 'Demande de congé',
    name_en = 'Leave request',
    description = 'Classification RH — demandes de congé.',
    direction_code = 'DAG',
    sort_order = 60,
    active = TRUE,
    updated_at = NOW(6)
WHERE id = @ft_id;

-- ═══════════════════════════════════════════════════════════════════════════
-- 2. Template de chaîne T-CONGE
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO chain_templates (
    id, code, name, description, file_type_code,
    total_delay_days, delay_unit, active, system_template,
    created_at, updated_at
)
SELECT
    @tpl_id,
    'T-CONGE',
    'Demande de congé (RH)',
    'Circuit RH portail interne — réception, validation N+1, clôture (4 j.o.).',
    'RH-CONGE',
    4,
    'WORKING_DAYS',
    TRUE,
    FALSE,
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-CONGE');

SET @tpl_id := (SELECT id FROM chain_templates WHERE code = 'T-CONGE' LIMIT 1);

UPDATE chain_templates
SET
    name = 'Demande de congé (RH)',
    description = 'Circuit RH portail interne — réception, validation N+1, clôture (4 j.o.).',
    file_type_code = 'RH-CONGE',
    total_delay_days = 4,
    delay_unit = 'WORKING_DAYS',
    active = TRUE,
    updated_at = NOW(6)
WHERE id = @tpl_id;

DELETE FROM chain_step_templates WHERE chain_template_id = @tpl_id;

INSERT INTO chain_step_templates (
    id, chain_template_id, step_order, label, responsible_role, organization_id,
    delay_value, delay_unit, expected_action, optional, closure_step,
    created_at, updated_at
) VALUES
(
    UNHEX(REPLACE('d1000000-0000-4000-8000-000000000711', '-', '')),
    @tpl_id, 1, 'Réception RH', 'SERVICE_HEAD',
    (SELECT id FROM organization WHERE code = 'DAG-RH' LIMIT 1),
    1, 'WORKING_DAYS', 'Enregistrer la demande et vérifier le dossier', FALSE, FALSE, NOW(6), NOW(6)
),
(
    UNHEX(REPLACE('d1000000-0000-4000-8000-000000000712', '-', '')),
    @tpl_id, 2, 'Validation N+1', 'DIRECTOR',
    (SELECT id FROM organization WHERE code = 'DAG-RH' LIMIT 1),
    3, 'WORKING_DAYS', 'Approuver ou refuser la demande de congé', FALSE, FALSE, NOW(6), NOW(6)
),
(
    UNHEX(REPLACE('d1000000-0000-4000-8000-000000000713', '-', '')),
    @tpl_id, 3, 'Clôture RH', 'SERVICE_HEAD',
    (SELECT id FROM organization WHERE code = 'DAG-RH' LIMIT 1),
    0, 'WORKING_DAYS', 'Notifier le demandeur et clôturer', FALSE, TRUE, NOW(6), NOW(6)
);

-- ═══════════════════════════════════════════════════════════════════════════
-- 3. Dossier préconfiguré RH-CONGE (formulaire + circuit)
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO preconfigured_dossiers (
    id, code, name, name_en, description,
    file_type_code, chain_template_id, default_first_step_responsible_user_id,
    direction_code, sort_order,
    active, portal_enabled, portal_audience,
    form_schema, required_attachment_keys,
    created_at, updated_at
)
SELECT
    @pd_id,
    'RH-CONGE',
    'Demande de congé',
    'Leave request',
    'Demande de congé agents — portail interne (pilote MVP). Circuit T-CONGE.',
    'RH-CONGE',
    @tpl_id,
    (SELECT id FROM users WHERE role = 'SERVICE_HEAD' AND active = TRUE ORDER BY created_at ASC LIMIT 1),
    'DAG',
    60,
    TRUE,
    TRUE,
    'INTERNAL',
    @form_schema,
    @required_attachment_keys,
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM preconfigured_dossiers WHERE code = 'RH-CONGE');

SET @pd_id := (SELECT id FROM preconfigured_dossiers WHERE code = 'RH-CONGE' LIMIT 1);

UPDATE preconfigured_dossiers
SET
    name = 'Demande de congé',
    name_en = 'Leave request',
    description = 'Demande de congé agents — portail interne (pilote MVP). Circuit T-CONGE.',
    file_type_code = 'RH-CONGE',
    chain_template_id = @tpl_id,
    default_first_step_responsible_user_id = COALESCE(
        default_first_step_responsible_user_id,
        (SELECT id FROM users WHERE role = 'SERVICE_HEAD' AND active = TRUE ORDER BY created_at ASC LIMIT 1)
    ),
    direction_code = 'DAG',
    sort_order = 60,
    active = TRUE,
    portal_enabled = TRUE,
    portal_audience = 'INTERNAL',
    form_schema = @form_schema,
    required_attachment_keys = @required_attachment_keys,
    updated_at = NOW(6)
WHERE id = @pd_id;

SELECT
    pd.code AS preconfigured_code,
    pd.portal_enabled,
    pd.portal_audience,
    pd.file_type_code,
    ct.code AS chain_code,
    CONCAT(u.first_name, ' ', u.last_name) AS first_responsible,
    CASE WHEN pd.form_schema IS NULL OR pd.form_schema = '' THEN 0 ELSE 1 END AS has_schema
FROM preconfigured_dossiers pd
LEFT JOIN chain_templates ct ON ct.id = pd.chain_template_id
LEFT JOIN users u ON u.id = pd.default_first_step_responsible_user_id
WHERE pd.code = 'RH-CONGE';
