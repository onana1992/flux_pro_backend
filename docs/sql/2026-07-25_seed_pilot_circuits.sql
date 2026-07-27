-- Objectif : Phase C — 6 circuits pilotes (compléter ENT-URG, DECOMPTE, AUTH-TRAV portail)
-- Déjà présents : COUR-STD/T01, MARCHE-SMP/T03, AUTH-TRAV/T04, RH-CONGE/T-CONGE
-- Ce script ajoute :
--   - ENT-URG + T-ENT-URG + préconfiguré EXTERNAL
--   - DECOMPTE + T-DECOMPTE + préconfiguré EXTERNAL
--   - AUTH-TRAV préconfiguré EXTERNAL (type+T04 déjà existants)
-- Prérequis : file_types, chain_templates, preconfigured_dossiers, users
-- Idempotent

-- ═══════════════════════════════════════════════════════════════════════════
-- A. ENT-URG — Signalement urgence / dégradation routière (portail EXTERNAL)
-- ═══════════════════════════════════════════════════════════════════════════

SET @ft_ent := UNHEX(REPLACE('e1000000-0000-4000-8000-000000000020', '-', ''));
SET @tpl_ent := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000801', '-', ''));
SET @pd_ent := UNHEX(REPLACE('f1000000-0000-4000-8000-000000000020', '-', ''));

SET @form_ent := '{
  "fields": [
    {"key": "localisation", "label": "Localisation (axe / PK / commune)", "type": "TEXT", "required": true, "maxLength": 255},
    {"key": "typeDegat", "label": "Type de dégradation", "type": "ENUM", "required": true,
     "options": ["NID_DE_POULE", "EFFONDREMENT", "GLISSEMENT", "PONT_ENDOMMAGE", "AUTRE"]},
    {"key": "urgence", "label": "Niveau d''urgence perçu", "type": "ENUM", "required": true,
     "options": ["NORMALE", "ELEVEE", "CRITIQUE"]},
    {"key": "description", "label": "Description", "type": "TEXT", "required": true, "maxLength": 1000},
    {"key": "contactPhone", "label": "Téléphone de contact", "type": "TEXT", "required": false, "maxLength": 40}
  ],
  "requiredAttachments": [
    {"key": "photos", "label": "Photos du constat", "minCount": 1}
  ]
}';

INSERT INTO file_types (
    id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at
)
SELECT @ft_ent, 'ENT-URG', 'Signalement urgence routière', 'Road emergency report',
       'Signalement de dégradation / urgence routière — DRTP. Circuit T-ENT-URG.',
       'DRTP-C', 70, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'ENT-URG');

SET @ft_ent := (SELECT id FROM file_types WHERE code = 'ENT-URG' LIMIT 1);

UPDATE file_types SET
    name = 'Signalement urgence routière',
    name_en = 'Road emergency report',
    description = 'Signalement de dégradation / urgence routière — DRTP. Circuit T-ENT-URG.',
    direction_code = 'DRTP-C',
    sort_order = 70,
    active = TRUE,
    updated_at = NOW(6)
WHERE id = @ft_ent;

INSERT INTO chain_templates (
    id, code, name, description, file_type_code,
    total_delay_days, delay_unit, active, system_template, created_at, updated_at
)
SELECT @tpl_ent, 'T-ENT-URG', 'Urgence routière DRTP',
       'Réception → constat terrain → décision → intervention → clôture (11 j.o.).',
       'ENT-URG', 11, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-ENT-URG');

SET @tpl_ent := (SELECT id FROM chain_templates WHERE code = 'T-ENT-URG' LIMIT 1);

UPDATE chain_templates SET
    name = 'Urgence routière DRTP',
    description = 'Réception → constat terrain → décision → intervention → clôture (11 j.o.).',
    file_type_code = 'ENT-URG',
    total_delay_days = 11,
    delay_unit = 'WORKING_DAYS',
    active = TRUE,
    updated_at = NOW(6)
WHERE id = @tpl_ent;

DELETE FROM chain_step_templates WHERE chain_template_id = @tpl_ent;

INSERT INTO chain_step_templates (
    id, chain_template_id, step_order, label, responsible_role,
    delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at
) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000811', '-', '')), @tpl_ent, 1, 'Réception signalement', 'SUPPORT',
 1, 'WORKING_DAYS', 'Enregistrer le signalement et vérifier les pièces', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000812', '-', '')), @tpl_ent, 2, 'Constat terrain DRTP', 'AGENT',
 2, 'WORKING_DAYS', 'Visiter le site et établir le constat', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000813', '-', '')), @tpl_ent, 3, 'Décision intervention', 'SERVICE_HEAD',
 2, 'WORKING_DAYS', 'Décider du type d''intervention et des moyens', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000814', '-', '')), @tpl_ent, 4, 'Exécution / suivi', 'AGENT',
 5, 'WORKING_DAYS', 'Suivre l''intervention et collecter les preuves', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000815', '-', '')), @tpl_ent, 5, 'Clôture', 'REGIONAL_DIRECTOR',
 1, 'WORKING_DAYS', 'Valider et clôturer le dossier', FALSE, TRUE, NOW(6), NOW(6));

INSERT INTO preconfigured_dossiers (
    id, code, name, name_en, description,
    file_type_code, chain_template_id, default_first_step_responsible_user_id,
    direction_code, sort_order, active, portal_enabled, portal_audience,
    form_schema, required_attachment_keys, created_at, updated_at
)
SELECT
    @pd_ent, 'ENT-URG', 'Signalement urgence routière', 'Road emergency report',
    'Portail externe — signalement dégradation / urgence. Circuit T-ENT-URG.',
    'ENT-URG', @tpl_ent,
    (SELECT id FROM users WHERE role IN ('SUPPORT', 'AGENT') AND active = TRUE ORDER BY created_at ASC LIMIT 1),
    'DRTP-C', 70, TRUE, TRUE, 'EXTERNAL',
    @form_ent, '["photos"]', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM preconfigured_dossiers WHERE code = 'ENT-URG');

SET @pd_ent := (SELECT id FROM preconfigured_dossiers WHERE code = 'ENT-URG' LIMIT 1);

UPDATE preconfigured_dossiers SET
    name = 'Signalement urgence routière',
    name_en = 'Road emergency report',
    description = 'Portail externe — signalement dégradation / urgence. Circuit T-ENT-URG.',
    file_type_code = 'ENT-URG',
    chain_template_id = @tpl_ent,
    direction_code = 'DRTP-C',
    sort_order = 70,
    active = TRUE,
    portal_enabled = TRUE,
    portal_audience = 'EXTERNAL',
    form_schema = @form_ent,
    required_attachment_keys = '["photos"]',
    updated_at = NOW(6)
WHERE id = @pd_ent;

-- ═══════════════════════════════════════════════════════════════════════════
-- B. DECOMPTE — Demande de paiement entrepreneur (portail EXTERNAL)
-- ═══════════════════════════════════════════════════════════════════════════

SET @ft_dec := UNHEX(REPLACE('e1000000-0000-4000-8000-000000000021', '-', ''));
SET @tpl_dec := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000901', '-', ''));
SET @pd_dec := UNHEX(REPLACE('f1000000-0000-4000-8000-000000000021', '-', ''));

SET @form_dec := '{
  "fields": [
    {"key": "referenceMarche", "label": "Référence du marché", "type": "TEXT", "required": true, "maxLength": 80},
    {"key": "numeroDecompte", "label": "N° de décompte", "type": "TEXT", "required": true, "maxLength": 40},
    {"key": "montant", "label": "Montant (FCFA)", "type": "NUMBER", "required": true},
    {"key": "periode", "label": "Période des travaux", "type": "TEXT", "required": true, "maxLength": 80},
    {"key": "entreprise", "label": "Raison sociale entreprise", "type": "TEXT", "required": true, "maxLength": 255},
    {"key": "observations", "label": "Observations", "type": "TEXT", "required": false, "maxLength": 500}
  ],
  "requiredAttachments": [
    {"key": "decompte", "label": "Décompte signé (PDF)", "minCount": 1},
    {"key": "pv", "label": "PV / attachement", "minCount": 0}
  ]
}';

INSERT INTO file_types (
    id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at
)
SELECT @ft_dec, 'DECOMPTE', 'Décompte / demande de paiement', 'Payment certificate request',
       'Décompte entrepreneur — vérification technique et visa financier. Circuit T-DECOMPTE.',
       'DIER', 75, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'DECOMPTE');

SET @ft_dec := (SELECT id FROM file_types WHERE code = 'DECOMPTE' LIMIT 1);

UPDATE file_types SET
    name = 'Décompte / demande de paiement',
    name_en = 'Payment certificate request',
    description = 'Décompte entrepreneur — vérification technique et visa financier. Circuit T-DECOMPTE.',
    direction_code = 'DIER',
    sort_order = 75,
    active = TRUE,
    updated_at = NOW(6)
WHERE id = @ft_dec;

INSERT INTO chain_templates (
    id, code, name, description, file_type_code,
    total_delay_days, delay_unit, active, system_template, created_at, updated_at
)
SELECT @tpl_dec, 'T-DECOMPTE', 'Décompte entrepreneur',
       'Dépôt → vérif technique → visa financier → directeur → transmission paiement (12 j.o.).',
       'DECOMPTE', 12, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-DECOMPTE');

SET @tpl_dec := (SELECT id FROM chain_templates WHERE code = 'T-DECOMPTE' LIMIT 1);

UPDATE chain_templates SET
    name = 'Décompte entrepreneur',
    description = 'Dépôt → vérif technique → visa financier → directeur → transmission paiement (12 j.o.).',
    file_type_code = 'DECOMPTE',
    total_delay_days = 12,
    delay_unit = 'WORKING_DAYS',
    active = TRUE,
    updated_at = NOW(6)
WHERE id = @tpl_dec;

DELETE FROM chain_step_templates WHERE chain_template_id = @tpl_dec;

INSERT INTO chain_step_templates (
    id, chain_template_id, step_order, label, responsible_role,
    delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at
) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000911', '-', '')), @tpl_dec, 1, 'Réception décompte', 'SUPPORT',
 1, 'WORKING_DAYS', 'Enregistrer le décompte et contrôler les pièces', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000912', '-', '')), @tpl_dec, 2, 'Vérification technique', 'AGENT',
 4, 'WORKING_DAYS', 'Vérifier quantités / attachements / conformité chantier', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000913', '-', '')), @tpl_dec, 3, 'Visa financier', 'SERVICE_HEAD',
 3, 'WORKING_DAYS', 'Viser le volet financier et budgétaire', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000914', '-', '')), @tpl_dec, 4, 'Validation Directeur', 'DIRECTOR',
 2, 'WORKING_DAYS', 'Valider le décompte', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000915', '-', '')), @tpl_dec, 5, 'Transmission paiement', 'SERVICE_HEAD',
 2, 'WORKING_DAYS', 'Transmettre pour paiement et clôturer', FALSE, TRUE, NOW(6), NOW(6));

INSERT INTO preconfigured_dossiers (
    id, code, name, name_en, description,
    file_type_code, chain_template_id, default_first_step_responsible_user_id,
    direction_code, sort_order, active, portal_enabled, portal_audience,
    form_schema, required_attachment_keys, created_at, updated_at
)
SELECT
    @pd_dec, 'DECOMPTE', 'Décompte / demande de paiement', 'Payment certificate request',
    'Portail externe — dépôts de décomptes entrepreneurs. Circuit T-DECOMPTE.',
    'DECOMPTE', @tpl_dec,
    (SELECT id FROM users WHERE role IN ('SUPPORT', 'AGENT') AND active = TRUE ORDER BY created_at ASC LIMIT 1),
    'DIER', 75, TRUE, TRUE, 'EXTERNAL',
    @form_dec, '["decompte"]', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM preconfigured_dossiers WHERE code = 'DECOMPTE');

SET @pd_dec := (SELECT id FROM preconfigured_dossiers WHERE code = 'DECOMPTE' LIMIT 1);

UPDATE preconfigured_dossiers SET
    name = 'Décompte / demande de paiement',
    name_en = 'Payment certificate request',
    description = 'Portail externe — dépôts de décomptes entrepreneurs. Circuit T-DECOMPTE.',
    file_type_code = 'DECOMPTE',
    chain_template_id = @tpl_dec,
    direction_code = 'DIER',
    sort_order = 75,
    active = TRUE,
    portal_enabled = TRUE,
    portal_audience = 'EXTERNAL',
    form_schema = @form_dec,
    required_attachment_keys = '["decompte"]',
    updated_at = NOW(6)
WHERE id = @pd_dec;

-- ═══════════════════════════════════════════════════════════════════════════
-- C. AUTH-TRAV — préconfiguré portail EXTERNAL (type + T04 déjà présents)
-- ═══════════════════════════════════════════════════════════════════════════

SET @tpl_auth := (SELECT id FROM chain_templates WHERE code = 'T04' LIMIT 1);
SET @pd_auth := UNHEX(REPLACE('f1000000-0000-4000-8000-000000000022', '-', ''));

SET @form_auth := '{
  "fields": [
    {"key": "raisonSociale", "label": "Demandeur / entreprise", "type": "TEXT", "required": true, "maxLength": 255},
    {"key": "localisation", "label": "Localisation des travaux", "type": "TEXT", "required": true, "maxLength": 255},
    {"key": "natureTravaux", "label": "Nature des travaux", "type": "TEXT", "required": true, "maxLength": 500},
    {"key": "dateDebut", "label": "Date de début souhaitée", "type": "DATE", "required": true},
    {"key": "dateFin", "label": "Date de fin souhaitée", "type": "DATE", "required": true},
    {"key": "emprise", "label": "Emprise (m / m²)", "type": "TEXT", "required": false, "maxLength": 80}
  ],
  "requiredAttachments": [
    {"key": "plans", "label": "Plans / croquis", "minCount": 1},
    {"key": "assurance", "label": "Attestation d''assurance", "minCount": 0}
  ]
}';

INSERT INTO preconfigured_dossiers (
    id, code, name, name_en, description,
    file_type_code, chain_template_id, default_first_step_responsible_user_id,
    direction_code, sort_order, active, portal_enabled, portal_audience,
    form_schema, required_attachment_keys, created_at, updated_at
)
SELECT
    @pd_auth, 'AUTH-TRAV', 'Autorisation travaux domaine public', 'Public domain works authorization',
    'Portail externe — autorisation de travaux sur le domaine public routier. Circuit T04.',
    'AUTH-TRAV', @tpl_auth,
    (SELECT id FROM users WHERE role IN ('SUPPORT', 'AGENT', 'REGIONAL_DIRECTOR') AND active = TRUE ORDER BY created_at ASC LIMIT 1),
    'DRTP-C', 40, TRUE, TRUE, 'EXTERNAL',
    @form_auth, '["plans"]', NOW(6), NOW(6)
WHERE @tpl_auth IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM preconfigured_dossiers WHERE code = 'AUTH-TRAV');

SET @pd_auth := (SELECT id FROM preconfigured_dossiers WHERE code = 'AUTH-TRAV' LIMIT 1);

UPDATE preconfigured_dossiers SET
    name = 'Autorisation travaux domaine public',
    name_en = 'Public domain works authorization',
    description = 'Portail externe — autorisation de travaux sur le domaine public routier. Circuit T04.',
    file_type_code = 'AUTH-TRAV',
    chain_template_id = COALESCE(@tpl_auth, chain_template_id),
    direction_code = 'DRTP-C',
    sort_order = 40,
    active = TRUE,
    portal_enabled = TRUE,
    portal_audience = 'EXTERNAL',
    form_schema = @form_auth,
    required_attachment_keys = '["plans"]',
    updated_at = NOW(6)
WHERE id = @pd_auth;

-- ═══════════════════════════════════════════════════════════════════════════
-- Vérification — 6 circuits pilotes
-- ═══════════════════════════════════════════════════════════════════════════

SELECT
    ft.code AS file_type,
    ct.code AS chain,
    pd.code AS preconfigured,
    pd.portal_enabled,
    pd.portal_audience,
    pd.direction_code
FROM file_types ft
LEFT JOIN chain_templates ct ON ct.file_type_code = ft.code AND ct.active = TRUE
LEFT JOIN preconfigured_dossiers pd ON pd.file_type_code = ft.code
WHERE ft.code IN ('COUR-STD', 'MARCHE-SMP', 'AUTH-TRAV', 'RH-CONGE', 'ENT-URG', 'DECOMPTE')
ORDER BY ft.sort_order, ft.code, ct.code;
