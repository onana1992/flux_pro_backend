-- Objectif : purge complète des dossiers / types / templates, puis seed réaliste MINTP
-- Tables impactées :
--   alerts, file_passages, file_attachments, files, file_number_sequences,
--   alert_rules, chain_step_templates, chain_templates, file_types
-- Prérequis :
--   - schéma DOS / CHN / ALR déjà créé
--   - organisations MINTP, DAG, DIER, DRTP-C présentes (ou créées en §0)
--   - au moins un utilisateur actif (idéalement docs/sql/2026-07-03_seed_users_all_roles.sql)
--   - contrainte parallélisme appliquée (docs/sql/2026-07-08_chain_parallel_stages.sql)
-- Exécution : MANUELLE sur MySQL / MariaDB (ddl-auto=none)
-- UUID : UNHEX(REPLACE('…','-','')) — compatible MariaDB 10.4
--
-- Contenu seed (pilote MINTP) :
--   Types   : COUR-STD, COUR-URG, MARCHE-SMP, AUTH-TRAV, COOP-PART
--   Chaînes : T01–T05 (système) + T06 (visas parallèles DIER)
--   Dossiers: scénarios DAG / DIER / DRTP (brouillon, en cours, clôturé, annulé, ON_HOLD)
--
-- Après exécution : redémarrer l'app est OK (FileTypeDataInitializer / ChainDataInitializer
-- ne recrée que les codes absents).

-- ═══════════════════════════════════════════════════════════════════════════
-- 0. Organisations pilotes minimales (si absentes)
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c1000000-0000-4000-8000-000000000001', '-', '')), 'DAG',
       'Direction des Affaires Générales',
       '00000000-0000-4000-8000-000000000002', mintp.id, TRUE, NOW(6), NOW(6)
FROM organization mintp
WHERE mintp.code = 'MINTP'
  AND NOT EXISTS (SELECT 1 FROM organization o WHERE o.code = 'DAG');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c1000000-0000-4000-8000-000000000002', '-', '')), 'DIER',
       'Direction des Investissements et de l''Entretien Routier',
       '00000000-0000-4000-8000-000000000002', mintp.id, TRUE, NOW(6), NOW(6)
FROM organization mintp
WHERE mintp.code = 'MINTP'
  AND NOT EXISTS (SELECT 1 FROM organization o WHERE o.code = 'DIER');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c1000000-0000-4000-8000-000000000003', '-', '')), 'DRTP-C',
       'Délégation Régionale des Travaux Publics — Centre',
       '00000000-0000-4000-8000-000000000005', mintp.id, TRUE, NOW(6), NOW(6)
FROM organization mintp
WHERE mintp.code = 'MINTP'
  AND NOT EXISTS (SELECT 1 FROM organization o WHERE o.code = 'DRTP-C');

SET @seed_user := COALESCE(
    (SELECT id FROM users WHERE email = 'support@mintp.cm' LIMIT 1),
    (SELECT id FROM users WHERE email = 'agent@mintp.cm' LIMIT 1),
    (SELECT id FROM users WHERE email = 'super.admin@mintp.cm' LIMIT 1),
    (SELECT id FROM users WHERE email = 'e.fotso@mintp.cm' LIMIT 1),
    (SELECT id FROM users WHERE active = TRUE LIMIT 1)
);

SET @u_support := COALESCE((SELECT id FROM users WHERE email = 'support@mintp.cm' LIMIT 1), @seed_user);
SET @u_service := COALESCE((SELECT id FROM users WHERE email = 'service.head@mintp.cm' LIMIT 1), @seed_user);
SET @u_director := COALESCE((SELECT id FROM users WHERE email = 'director@mintp.cm' LIMIT 1), @seed_user);
SET @u_agent := COALESCE((SELECT id FROM users WHERE email = 'agent@mintp.cm' LIMIT 1), @seed_user);
SET @u_sg := COALESCE((SELECT id FROM users WHERE email = 'secretary.general@mintp.cm' LIMIT 1), @seed_user);
SET @u_rdir := COALESCE((SELECT id FROM users WHERE email = 'regional.director@mintp.cm' LIMIT 1), @seed_user);

-- ═══════════════════════════════════════════════════════════════════════════
-- 1. PURGE (ordre FK)
-- ═══════════════════════════════════════════════════════════════════════════

DELETE FROM alerts WHERE file_id IS NOT NULL OR file_passage_id IS NOT NULL;

DELETE FROM file_passages;
DELETE FROM file_attachments;
DELETE FROM files;
DELETE FROM file_number_sequences;

DELETE FROM alert_rules;

DELETE FROM chain_step_templates;
DELETE FROM chain_templates;
DELETE FROM file_types;

-- ═══════════════════════════════════════════════════════════════════════════
-- 2. TYPES DE DOSSIERS MINTP
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO file_types (
    id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at
) VALUES
(
    UNHEX(REPLACE('e1000000-0000-4000-8000-000000000001', '-', '')),
    'COUR-STD', 'Courrier entrant standard', 'Standard incoming mail',
    'Correspondances et courriers entrants traités par la DAG (Service Courrier). Circuit T01.',
    'DAG', 10, TRUE, NOW(6), NOW(6)
),
(
    UNHEX(REPLACE('e1000000-0000-4000-8000-000000000002', '-', '')),
    'COUR-URG', 'Courrier très urgent', 'Very urgent incoming mail',
    'Courrier entrant à délai accéléré (heures ouvrées). Circuit T02 — priorité VERY_URGENT.',
    'DAG', 20, TRUE, NOW(6), NOW(6)
),
(
    UNHEX(REPLACE('e1000000-0000-4000-8000-000000000003', '-', '')),
    'MARCHE-SMP', 'Marché public simplifié', 'Simplified public procurement',
    'Marchés publics sous seuil — consultation simplifiée DIER. Circuits T03 / T06.',
    'DIER', 30, TRUE, NOW(6), NOW(6)
),
(
    UNHEX(REPLACE('e1000000-0000-4000-8000-000000000004', '-', '')),
    'AUTH-TRAV', 'Autorisation travaux domaine public', 'Public domain works authorization',
    'Autorisations d''occupation / travaux sur le domaine public routier (DRTP Centre). Circuit T04.',
    'DRTP-C', 40, TRUE, NOW(6), NOW(6)
),
(
    UNHEX(REPLACE('e1000000-0000-4000-8000-000000000005', '-', '')),
    'COOP-PART', 'Coopération / partenariat', 'Cooperation / partnership',
    'Dossiers de coopération internationale — hors périmètre pilote actif. Circuit T05 (inactif).',
    NULL, 50, FALSE, NOW(6), NOW(6)
);

-- ═══════════════════════════════════════════════════════════════════════════
-- 3. TEMPLATES DE CHAÎNE + MAILLONS
-- ═══════════════════════════════════════════════════════════════════════════

SET @t01 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000101', '-', ''));
SET @t02 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000201', '-', ''));
SET @t03 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000301', '-', ''));
SET @t04 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000401', '-', ''));
SET @t05 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000501', '-', ''));
SET @t06 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000601', '-', ''));

INSERT INTO chain_templates (
    id, code, name, description, file_type_code,
    total_delay_days, delay_unit, active, system_template, created_at, updated_at
) VALUES
(@t01, 'T01', 'Courrier entrant standard',
 'Circuit DAG — réception, orientation, traitement, validation, expédition (11 j.o.).',
 'COUR-STD', 11, 'WORKING_DAYS', TRUE, TRUE, NOW(6), NOW(6)),
(@t02, 'T02', 'Courrier très urgent',
 'Circuit accéléré DAG — délais en heures ouvrées.',
 'COUR-URG', 3, 'WORKING_HOURS', TRUE, TRUE, NOW(6), NOW(6)),
(@t03, 'T03', 'Marché public simplifié',
 'Circuit DIER — instruction, visas financier / SG / Cabinet, notification (21 j.o.).',
 'MARCHE-SMP', 21, 'WORKING_DAYS', TRUE, TRUE, NOW(6), NOW(6)),
(@t04, 'T04', 'Autorisation travaux DRTP',
 'Circuit DRTP Centre — instruction, visite terrain, validation, délivrance (18 j.o.).',
 'AUTH-TRAV', 18, 'WORKING_DAYS', TRUE, TRUE, NOW(6), NOW(6)),
(@t05, 'T05', 'Coopération / partenariat',
 'Circuit coopération internationale — hors pilote (inactif).',
 'COOP-PART', 10, 'WORKING_DAYS', FALSE, TRUE, NOW(6), NOW(6)),
(@t06, 'T06', 'Marché — visas parallèles',
 'Variante DIER : trois visas simultanés (jonction ET) avant validation SG.',
 'MARCHE-SMP', 8, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6));

INSERT INTO chain_step_templates (
    id, chain_template_id, step_order, label, responsible_role,
    delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at
) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000111', '-', '')), @t01, 1, 'Réception DAG', 'SUPPORT',
 1, 'WORKING_DAYS', 'Enregistrer et numériser le courrier', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000112', '-', '')), @t01, 2, 'Orientation Chef Courrier', 'SERVICE_HEAD',
 1, 'WORKING_DAYS', 'Orienter vers la direction destinataire', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000113', '-', '')), @t01, 3, 'Directeur destinataire', 'DIRECTOR',
 1, 'WORKING_DAYS', 'Désigner l''agent traitant', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000114', '-', '')), @t01, 4, 'Agent traitant', 'AGENT',
 5, 'WORKING_DAYS', 'Traiter le dossier et préparer le projet de réponse', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000115', '-', '')), @t01, 5, 'Validation Chef Service', 'SERVICE_HEAD',
 2, 'WORKING_DAYS', 'Valider le projet de réponse', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000116', '-', '')), @t01, 6, 'Expédition réponse', 'SUPPORT',
 1, 'WORKING_DAYS', 'Expédier la réponse et archiver le scan', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000117', '-', '')), @t01, 7, 'Clôture', 'AGENT',
 0, 'WORKING_DAYS', 'Clôturer le dossier', FALSE, TRUE, NOW(6), NOW(6));

INSERT INTO chain_step_templates (
    id, chain_template_id, step_order, label, responsible_role,
    delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at
) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000211', '-', '')), @t02, 1, 'Réception DAG', 'SUPPORT',
 4, 'WORKING_HOURS', 'Enregistrer et numériser en urgence', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000212', '-', '')), @t02, 2, 'Directeur destinataire', 'DIRECTOR',
 4, 'WORKING_HOURS', 'Désigner immédiatement l''agent traitant', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000213', '-', '')), @t02, 3, 'Agent traitant', 'AGENT',
 1, 'WORKING_DAYS', 'Traiter et préparer la réponse sous délai court', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000214', '-', '')), @t02, 4, 'Validation', 'SERVICE_HEAD',
 4, 'WORKING_HOURS', 'Valider le projet de réponse', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000215', '-', '')), @t02, 5, 'Expédition', 'SUPPORT',
 4, 'WORKING_HOURS', 'Expédier et archiver', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000216', '-', '')), @t02, 6, 'Clôture', 'AGENT',
 0, 'WORKING_DAYS', 'Clôturer le dossier', FALSE, TRUE, NOW(6), NOW(6));

INSERT INTO chain_step_templates (
    id, chain_template_id, step_order, label, responsible_role,
    delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at
) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000311', '-', '')), @t03, 1, 'Enregistrement et visa SG', 'SUPPORT',
 1, 'WORKING_DAYS', 'Enregistrer le dossier marché et transmettre', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000312', '-', '')), @t03, 2, 'Instruction technique', 'AGENT',
 5, 'WORKING_DAYS', 'Instruire techniquement (cahier des charges, devis)', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000313', '-', '')), @t03, 3, 'Visa financier', 'SERVICE_HEAD',
 3, 'WORKING_DAYS', 'Viser le volet financier et budgétaire', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000314', '-', '')), @t03, 4, 'Validation Directeur DIER', 'DIRECTOR',
 2, 'WORKING_DAYS', 'Valider l''instruction technique et financière', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000315', '-', '')), @t03, 5, 'Avis SG', 'SECRETARY_GENERAL',
 3, 'WORKING_DAYS', 'Émettre l''avis du Secrétaire Général', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000316', '-', '')), @t03, 6, 'Visa Ministre (si requis)', 'EXECUTIVE_OFFICE',
 5, 'WORKING_DAYS', 'Visa Cabinet / Ministre si le seuil l''exige', TRUE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000317', '-', '')), @t03, 7, 'Notification et archivage', 'SUPPORT',
 2, 'WORKING_DAYS', 'Notifier l''attributaire et archiver', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000318', '-', '')), @t03, 8, 'Clôture', 'AGENT',
 0, 'WORKING_DAYS', 'Clôturer le dossier', FALSE, TRUE, NOW(6), NOW(6));

INSERT INTO chain_step_templates (
    id, chain_template_id, step_order, label, responsible_role,
    delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at
) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000411', '-', '')), @t04, 1, 'Réception demande', 'SUPPORT',
 1, 'WORKING_DAYS', 'Enregistrer la demande d''autorisation', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000412', '-', '')), @t04, 2, 'Instruction technique', 'AGENT',
 7, 'WORKING_DAYS', 'Instruire techniquement (emprise, sécurité routière)', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000413', '-', '')), @t04, 3, 'Visite terrain', 'SERVICE_HEAD',
 5, 'WORKING_DAYS', 'Organiser et réaliser la visite terrain', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000414', '-', '')), @t04, 4, 'Validation Directeur DRTP', 'REGIONAL_DIRECTOR',
 3, 'WORKING_DAYS', 'Valider l''instruction et autoriser', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000415', '-', '')), @t04, 5, 'Délivrance autorisation', 'SUPPORT',
 2, 'WORKING_DAYS', 'Délivrer l''autorisation au demandeur', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000416', '-', '')), @t04, 6, 'Clôture', 'AGENT',
 0, 'WORKING_DAYS', 'Clôturer le dossier', FALSE, TRUE, NOW(6), NOW(6));

INSERT INTO chain_step_templates (
    id, chain_template_id, step_order, label, responsible_role,
    delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at
) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000511', '-', '')), @t05, 1, 'Réception dossier', 'SUPPORT',
 2, 'WORKING_DAYS', 'Enregistrer le dossier de coopération', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000512', '-', '')), @t05, 2, 'Instruction', 'AGENT',
 8, 'WORKING_DAYS', 'Instruire le partenariat', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000513', '-', '')), @t05, 3, 'Clôture', 'AGENT',
 0, 'WORKING_DAYS', 'Clôturer le dossier', FALSE, TRUE, NOW(6), NOW(6));

INSERT INTO chain_step_templates (
    id, chain_template_id, step_order, label, responsible_role,
    delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at
) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000611', '-', '')), @t06, 1, 'Enregistrement DAG', 'SUPPORT',
 1, 'WORKING_DAYS', 'Enregistrer le dossier et affecter le numéro', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000621', '-', '')), @t06, 2, 'Visa technique DIER', 'SERVICE_HEAD',
 5, 'WORKING_DAYS', 'Instruire et viser techniquement', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000622', '-', '')), @t06, 2, 'Visa financier DAF', 'DIRECTOR',
 3, 'WORKING_DAYS', 'Viser le volet financier et budgétaire', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000623', '-', '')), @t06, 2, 'Avis juridique', 'AGENT',
 4, 'WORKING_DAYS', 'Émettre l''avis juridique', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000631', '-', '')), @t06, 3, 'Validation Secrétaire Général', 'SECRETARY_GENERAL',
 2, 'WORKING_DAYS', 'Valider l''instruction consolidée', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000641', '-', '')), @t06, 4, 'Clôture', 'AGENT',
 0, 'WORKING_DAYS', 'Clôturer le dossier', FALSE, TRUE, NOW(6), NOW(6));

-- 3bis. Règles d'alerte T01–T06 :
--      exécuter ensuite docs/sql/2026-07-14_seed_alert_rules_t01_t06.sql
--      (CDC §10.2 en j.o. ; profil heures pour T02)

-- ═══════════════════════════════════════════════════════════════════════════
-- 4. DOSSIERS DE DÉMONSTRATION
-- ═══════════════════════════════════════════════════════════════════════════

SET @org_dag := (SELECT id FROM organization WHERE code = 'DAG' LIMIT 1);
SET @org_dier := (SELECT id FROM organization WHERE code = 'DIER' LIMIT 1);
SET @org_drtp := (SELECT id FROM organization WHERE code = 'DRTP-C' LIMIT 1);

INSERT INTO files (
    id, reference_number, file_type_code, chain_template_id,
    organization_id, created_by_user_id,
    subject, sender_or_beneficiary, received_at,
    priority, status,
    closure_reason, closed_at,
    cancellation_reason, cancelled_at,
    external_hold_reason, external_hold_since,
    metadata, created_at, updated_at
) VALUES
(
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000001', '-', '')),
    NULL, 'COUR-STD', NULL, @org_dag, @u_support,
    'Demande de renseignements — permis de construire route communale',
    'Mairie d''Obala', '2026-07-01', 'NORMAL', 'DRAFT',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NOW(6), NOW(6)
),
(
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000002', '-', '')),
    'MINTP-DAG-2026-0001', 'COUR-STD', @t01, @org_dag, @u_support,
    'Invitation cérémonie pose de première pierre — pont sur le Nyong',
    'Ministère des Finances', '2026-06-12', 'NORMAL', 'IN_PROGRESS',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NOW(6), NOW(6)
),
(
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000003', '-', '')),
    'MINTP-DAG-2026-0002', 'COUR-URG', @t02, @org_dag, @u_support,
    'Courrier recommandé — société privée BTP (réponse sous 48 h)',
    'Société Cameroun BTP SARL', '2026-07-08', 'VERY_URGENT', 'IN_PROGRESS',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NOW(6), NOW(6)
),
(
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000004', '-', '')),
    'MINTP-DIER-2026-0001', 'MARCHE-SMP', @t03, @org_dier, @u_agent,
    'Marché simplifié — entretien RN3 tronçon Mbalmayo–Ngoumou',
    'Entreprise Routière du Centre', '2026-06-05', 'NORMAL', 'IN_PROGRESS',
    NULL, NULL, NULL, NULL, NULL, NULL,
    JSON_OBJECT('montantEstime', 45000000, 'devise', 'XAF', 'troncon', 'RN3'),
    NOW(6), NOW(6)
),
(
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000005', '-', '')),
    'MINTP-DIER-2026-0002', 'MARCHE-SMP', @t06, @org_dier, @u_agent,
    'Marché simplifié — signalisation horizontale axe Yaoundé–Mfou',
    'Ets Signal Routes Cameroun', '2026-06-28', 'URGENT', 'IN_PROGRESS',
    NULL, NULL, NULL, NULL, NULL, NULL,
    JSON_OBJECT('montantEstime', 18500000, 'devise', 'XAF', 'circuit', 'T06-paralleles'),
    NOW(6), NOW(6)
),
(
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000006', '-', '')),
    'MINTP-DRTP-C-2026-0001', 'AUTH-TRAV', @t04, @org_drtp, @u_support,
    'Autorisation travaux — tranchée fibre optique Boulevard du 20 Mai',
    'CAMTEL', '2026-06-20', 'URGENT', 'IN_PROGRESS',
    NULL, NULL, NULL, NULL, NULL, NULL,
    JSON_OBJECT('localisation', 'Yaoundé — Bastos', 'dureeJours', 14),
    NOW(6), NOW(6)
),
(
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000007', '-', '')),
    NULL, 'AUTH-TRAV', NULL, @org_drtp, @u_support,
    'Demande autorisation — pose signalisation provisoire chantier',
    'Commune de Yaoundé V', '2026-07-05', 'NORMAL', 'DRAFT',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NOW(6), NOW(6)
),
(
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000008', '-', '')),
    'MINTP-DAG-2026-0003', 'COUR-STD', @t01, @org_dag, @u_support,
    'Transmission compte rendu réunion interministérielle infrastructures',
    'Primature', '2026-05-28', 'NORMAL', 'CLOSED',
    'Réponse signée et expédiée au Cabinet — AR reçu le 15/06/2026',
    '2026-06-15 16:30:00.000000',
    NULL, NULL, NULL, NULL, NULL, NOW(6), NOW(6)
),
(
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000009', '-', '')),
    NULL, 'COUR-STD', NULL, @org_dag, @u_support,
    'Doublon — même courrier déjà enregistré sous MINTP-DAG-2026-0001',
    'Cabinet partenaire', '2026-06-14', 'NORMAL', 'CANCELLED',
    NULL, NULL,
    'Dossier en double détecté à la réception — fusion avec MINTP-DAG-2026-0001',
    '2026-06-14 11:00:00.000000',
    NULL, NULL, NULL, NOW(6), NOW(6)
),
(
    UNHEX(REPLACE('a1000000-0000-4000-8000-00000000000a', '-', '')),
    'MINTP-DRTP-C-2026-0002', 'AUTH-TRAV', @t04, @org_drtp, @u_agent,
    'Autorisation travaux — raccordement adduction d''eau RN1 PK 12',
    'Camerounaise des Eaux (CDE)', '2026-06-10', 'NORMAL', 'ON_HOLD',
    NULL, NULL, NULL, NULL,
    'En attente plan de circulation provisoire du demandeur',
    '2026-07-02 09:15:00.000000',
    JSON_OBJECT('localisation', 'RN1 PK12 — Soa', 'dureeJours', 21),
    NOW(6), NOW(6)
);

INSERT INTO file_number_sequences (organization_id, year, last_sequence)
VALUES
    (@org_dag, 2026, 3),
    (@org_dier, 2026, 2),
    (@org_drtp, 2026, 2)
ON DUPLICATE KEY UPDATE last_sequence = VALUES(last_sequence);

-- ═══════════════════════════════════════════════════════════════════════════
-- 5. CIRCUITS INITIALISÉS (échantillons)
-- ═══════════════════════════════════════════════════════════════════════════

SET @f02 := UNHEX(REPLACE('a1000000-0000-4000-8000-000000000002', '-', ''));
SET @s111 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000111', '-', ''));
SET @s112 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000112', '-', ''));
SET @s113 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000113', '-', ''));
SET @s114 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000114', '-', ''));
SET @s115 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000115', '-', ''));
SET @s116 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000116', '-', ''));
SET @s117 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000117', '-', ''));

INSERT INTO file_passages (
    id, file_id, chain_step_template_id, step_order, responsible_user_id,
    status, received_at, transmitted_at, due_at, created_at, updated_at
) VALUES
(UNHEX(REPLACE('f1000000-0000-4000-8000-000000000201', '-', '')), @f02, @s111, 1, @u_support,
 'COMPLETED', '2026-06-12 08:00:00', '2026-06-12 15:30:00', '2026-06-13 17:00:00', NOW(6), NOW(6)),
(UNHEX(REPLACE('f1000000-0000-4000-8000-000000000202', '-', '')), @f02, @s112, 2, @u_service,
 'IN_PROGRESS', '2026-06-12 15:30:00', NULL, '2026-06-13 17:00:00', NOW(6), NOW(6)),
(UNHEX(REPLACE('f1000000-0000-4000-8000-000000000203', '-', '')), @f02, @s113, 3, @u_director,
 'PENDING', NULL, NULL, NULL, NOW(6), NOW(6)),
(UNHEX(REPLACE('f1000000-0000-4000-8000-000000000204', '-', '')), @f02, @s114, 4, @u_agent,
 'PENDING', NULL, NULL, NULL, NOW(6), NOW(6)),
(UNHEX(REPLACE('f1000000-0000-4000-8000-000000000205', '-', '')), @f02, @s115, 5, @u_service,
 'PENDING', NULL, NULL, NULL, NOW(6), NOW(6)),
(UNHEX(REPLACE('f1000000-0000-4000-8000-000000000206', '-', '')), @f02, @s116, 6, @u_support,
 'PENDING', NULL, NULL, NULL, NOW(6), NOW(6)),
(UNHEX(REPLACE('f1000000-0000-4000-8000-000000000207', '-', '')), @f02, @s117, 7, @u_agent,
 'PENDING', NULL, NULL, NULL, NOW(6), NOW(6));

SET @f05 := UNHEX(REPLACE('a1000000-0000-4000-8000-000000000005', '-', ''));
SET @s611 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000611', '-', ''));
SET @s621 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000621', '-', ''));
SET @s622 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000622', '-', ''));
SET @s623 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000623', '-', ''));
SET @s631 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000631', '-', ''));
SET @s641 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000641', '-', ''));

INSERT INTO file_passages (
    id, file_id, chain_step_template_id, step_order, responsible_user_id,
    status, received_at, transmitted_at, due_at, created_at, updated_at
) VALUES
(UNHEX(REPLACE('f1000000-0000-4000-8000-000000000501', '-', '')), @f05, @s611, 1, @u_support,
 'COMPLETED', '2026-06-28 08:00:00', '2026-06-28 16:00:00', '2026-06-29 17:00:00', NOW(6), NOW(6)),
(UNHEX(REPLACE('f1000000-0000-4000-8000-000000000502', '-', '')), @f05, @s621, 2, @u_service,
 'IN_PROGRESS', '2026-06-28 16:00:00', NULL, '2026-07-03 17:00:00', NOW(6), NOW(6)),
(UNHEX(REPLACE('f1000000-0000-4000-8000-000000000503', '-', '')), @f05, @s622, 2, @u_director,
 'IN_PROGRESS', '2026-06-28 16:00:00', NULL, '2026-07-01 17:00:00', NOW(6), NOW(6)),
(UNHEX(REPLACE('f1000000-0000-4000-8000-000000000504', '-', '')), @f05, @s623, 2, @u_agent,
 'IN_PROGRESS', '2026-06-28 16:00:00', NULL, '2026-07-02 17:00:00', NOW(6), NOW(6)),
(UNHEX(REPLACE('f1000000-0000-4000-8000-000000000505', '-', '')), @f05, @s631, 3, @u_sg,
 'PENDING', NULL, NULL, NULL, NOW(6), NOW(6)),
(UNHEX(REPLACE('f1000000-0000-4000-8000-000000000506', '-', '')), @f05, @s641, 4, @u_agent,
 'PENDING', NULL, NULL, NULL, NOW(6), NOW(6));

SET @f06 := UNHEX(REPLACE('a1000000-0000-4000-8000-000000000006', '-', ''));
SET @s411 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000411', '-', ''));
SET @s412 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000412', '-', ''));
SET @s413 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000413', '-', ''));
SET @s414 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000414', '-', ''));
SET @s415 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000415', '-', ''));
SET @s416 := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000416', '-', ''));

INSERT INTO file_passages (
    id, file_id, chain_step_template_id, step_order, responsible_user_id,
    status, received_at, transmitted_at, due_at, created_at, updated_at
) VALUES
(UNHEX(REPLACE('f1000000-0000-4000-8000-000000000601', '-', '')), @f06, @s411, 1, @u_support,
 'COMPLETED', '2026-06-20 08:30:00', '2026-06-20 17:00:00', '2026-06-22 17:00:00', NOW(6), NOW(6)),
(UNHEX(REPLACE('f1000000-0000-4000-8000-000000000602', '-', '')), @f06, @s412, 2, @u_agent,
 'IN_PROGRESS', '2026-06-20 17:00:00', NULL, '2026-06-30 17:00:00', NOW(6), NOW(6)),
(UNHEX(REPLACE('f1000000-0000-4000-8000-000000000603', '-', '')), @f06, @s413, 3, @u_service,
 'PENDING', NULL, NULL, NULL, NOW(6), NOW(6)),
(UNHEX(REPLACE('f1000000-0000-4000-8000-000000000604', '-', '')), @f06, @s414, 4, @u_rdir,
 'PENDING', NULL, NULL, NULL, NOW(6), NOW(6)),
(UNHEX(REPLACE('f1000000-0000-4000-8000-000000000605', '-', '')), @f06, @s415, 5, @u_support,
 'PENDING', NULL, NULL, NULL, NOW(6), NOW(6)),
(UNHEX(REPLACE('f1000000-0000-4000-8000-000000000606', '-', '')), @f06, @s416, 6, @u_agent,
 'PENDING', NULL, NULL, NULL, NOW(6), NOW(6));

-- Vérifications :
-- SELECT code, name, direction_code, active FROM file_types ORDER BY sort_order;
-- SELECT code, name, file_type_code, active FROM chain_templates ORDER BY code;
-- SELECT reference_number, file_type_code, status, priority FROM files ORDER BY created_at;

-- rollback (commenté) :
-- DELETE FROM alerts WHERE file_id IS NOT NULL OR file_passage_id IS NOT NULL;
-- DELETE FROM file_passages; DELETE FROM file_attachments; DELETE FROM files;
-- DELETE FROM file_number_sequences; DELETE FROM alert_rules;
-- DELETE FROM chain_step_templates; DELETE FROM chain_templates; DELETE FROM file_types;
