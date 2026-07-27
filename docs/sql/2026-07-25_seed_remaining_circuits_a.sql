-- Objectif : rester des 30 circuits (hors 6 pilotes déjà seedés)
-- Déjà OK : COUR-STD, COUR-URG, MARCHE-SMP, AUTH-TRAV, RH-CONGE, ENT-URG, DECOMPTE, COOP-PART/T05
-- Ajoute file_types + chain_templates (+ préconfigurés portail si INT/EXT)
-- Idempotent

-- ═══════════════════════════════════════════════════════════════════════════
-- Helper macro (commentaire) : insert type if missing
-- ═══════════════════════════════════════════════════════════════════════════

-- NOTE-CAB
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000030', '-', '')),
       'NOTE-CAB', 'Note au Ministre / SG', 'Note to Minister / SG',
       'Note d''arbitrage Cabinet / SG.', 'MINTP-SG', 15, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'NOTE-CAB');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000001001', '-', '')),
       'T-NOTE-CAB', 'Note Cabinet / SG', 'Direction → DG → SG → Cabinet → retour (12 j.o.).',
       'NOTE-CAB', 12, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-NOTE-CAB');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-NOTE-CAB' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001011', '-', '')), @t, 1, 'Rédaction direction', 'DIRECTOR', 2, 'WORKING_DAYS', 'Préparer la note', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001012', '-', '')), @t, 2, 'Visa DG / Directeur', 'DIRECTOR', 2, 'WORKING_DAYS', 'Viser la note', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001013', '-', '')), @t, 3, 'Secrétariat Général', 'SECRETARY_GENERAL', 3, 'WORKING_DAYS', 'Instruire / transmettre', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001014', '-', '')), @t, 4, 'Cabinet', 'EXECUTIVE_OFFICE', 3, 'WORKING_DAYS', 'Arbitrer / instruire', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001015', '-', '')), @t, 5, 'Retour instruction', 'SUPPORT', 2, 'WORKING_DAYS', 'Notifier et clôturer', FALSE, TRUE, NOW(6), NOW(6));

-- AUD-CAB (portail EXTERNAL)
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000031', '-', '')),
       'AUD-CAB', 'Demande d''audience', 'Audience request',
       'Demande d''audience Cabinet.', 'MINTP-CABINET', 16, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'AUD-CAB');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000001101', '-', '')),
       'T-AUD-CAB', 'Audience Cabinet', 'Réception → filtrage → agenda → notification (7 j.o.).',
       'AUD-CAB', 7, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-AUD-CAB');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-AUD-CAB' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001111', '-', '')), @t, 1, 'Réception', 'SUPPORT', 1, 'WORKING_DAYS', 'Enregistrer la demande', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001112', '-', '')), @t, 2, 'Filtrage Cabinet', 'EXECUTIVE_OFFICE', 2, 'WORKING_DAYS', 'Filtrer / prioriser', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001113', '-', '')), @t, 3, 'Décision agenda', 'EXECUTIVE_OFFICE', 3, 'WORKING_DAYS', 'Fixer la date', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001114', '-', '')), @t, 4, 'Notification', 'SUPPORT', 1, 'WORKING_DAYS', 'Notifier le demandeur', FALSE, TRUE, NOW(6), NOW(6));

INSERT INTO preconfigured_dossiers (id, code, name, name_en, description, file_type_code, chain_template_id, direction_code, sort_order, active, portal_enabled, portal_audience, form_schema, required_attachment_keys, created_at, updated_at)
SELECT UNHEX(REPLACE('f1000000-0000-4000-8000-000000000031', '-', '')),
       'AUD-CAB', 'Demande d''audience', 'Audience request',
       'Portail externe — demande d''audience.', 'AUD-CAB', @t, 'MINTP-CABINET', 16, TRUE, TRUE, 'EXTERNAL',
       '{"fields":[{"key":"motif","label":"Motif de l''audience","type":"TEXT","required":true,"maxLength":500},{"key":"organisation","label":"Organisation","type":"TEXT","required":true,"maxLength":255},{"key":"contact","label":"Contact","type":"TEXT","required":true,"maxLength":120}]}',
       '[]', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM preconfigured_dossiers WHERE code = 'AUD-CAB');

-- COUR-OUT
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000032', '-', '')),
       'COUR-OUT', 'Courrier sortant à viser', 'Outgoing mail to endorse',
       'Projet de courrier sortant à viser.', 'DAG', 18, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'COUR-OUT');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000001201', '-', '')),
       'T-COUR-OUT', 'Courrier sortant', 'Rédacteur → chef → directeur → SG? → expédition (8 j.o.).',
       'COUR-OUT', 8, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-COUR-OUT');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-COUR-OUT' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001211', '-', '')), @t, 1, 'Rédaction', 'AGENT', 2, 'WORKING_DAYS', 'Rédiger le projet', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001212', '-', '')), @t, 2, 'Visa chef de service', 'SERVICE_HEAD', 1, 'WORKING_DAYS', 'Viser', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001213', '-', '')), @t, 3, 'Visa directeur', 'DIRECTOR', 2, 'WORKING_DAYS', 'Viser', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001214', '-', '')), @t, 4, 'Visa SG (si requis)', 'SECRETARY_GENERAL', 2, 'WORKING_DAYS', 'Viser si requis', TRUE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001215', '-', '')), @t, 5, 'Expédition', 'SUPPORT', 1, 'WORKING_DAYS', 'Expédier et archiver', FALSE, TRUE, NOW(6), NOW(6));

-- DAO-VAL
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000033', '-', '')),
       'DAO-VAL', 'Validation DAO', 'Tender dossier validation',
       'Validation dossier d''appel d''offres.', 'DIER', 31, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'DAO-VAL');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000001301', '-', '')),
       'T-DAO-VAL', 'Validation DAO', 'Projet → direction → marchés → DG → SG (13 j.o.).',
       'DAO-VAL', 13, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-DAO-VAL');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-DAO-VAL' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001311', '-', '')), @t, 1, 'Service projet', 'AGENT', 3, 'WORKING_DAYS', 'Constituer le DAO', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001312', '-', '')), @t, 2, 'Direction technique', 'SERVICE_HEAD', 3, 'WORKING_DAYS', 'Valider techniquement', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001313', '-', '')), @t, 3, 'Service marchés', 'SERVICE_HEAD', 3, 'WORKING_DAYS', 'Contrôler la conformité marchés', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001314', '-', '')), @t, 4, 'Visa DG', 'DIRECTOR', 2, 'WORKING_DAYS', 'Viser', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001315', '-', '')), @t, 5, 'Visa SG', 'SECRETARY_GENERAL', 2, 'WORKING_DAYS', 'Viser et clôturer', FALSE, TRUE, NOW(6), NOW(6));

-- AVENANT
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000034', '-', '')),
       'AVENANT', 'Avenant de marché', 'Contract amendment',
       'Instruction et visas d''avenant.', 'DIER', 32, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'AVENANT');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000001401', '-', '')),
       'T-AVENANT', 'Avenant de marché', 'Projet → directeur → juridique → SG → notification (13 j.o.).',
       'AVENANT', 13, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-AVENANT');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-AVENANT' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001411', '-', '')), @t, 1, 'Chef projet', 'AGENT', 3, 'WORKING_DAYS', 'Préparer l''avenant', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001412', '-', '')), @t, 2, 'Directeur', 'DIRECTOR', 3, 'WORKING_DAYS', 'Valider', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001413', '-', '')), @t, 3, 'Affaires juridiques', 'AGENT', 3, 'WORKING_DAYS', 'Avis juridique', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001414', '-', '')), @t, 4, 'SG', 'SECRETARY_GENERAL', 3, 'WORKING_DAYS', 'Viser', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001415', '-', '')), @t, 5, 'Notification', 'SUPPORT', 1, 'WORKING_DAYS', 'Notifier', FALSE, TRUE, NOW(6), NOW(6));

-- PV-RECEP
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000035', '-', '')),
       'PV-RECEP', 'PV de réception de travaux', 'Works acceptance report',
       'Réception de travaux / PV.', 'DIER', 33, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'PV-RECEP');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000001501', '-', '')),
       'T-PV-RECEP', 'PV réception', 'Contrôle → chef → directeur → commission → clôture (14 j.o.).',
       'PV-RECEP', 14, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-PV-RECEP');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-PV-RECEP' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001511', '-', '')), @t, 1, 'Contrôle terrain', 'AGENT', 3, 'WORKING_DAYS', 'Constater', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001512', '-', '')), @t, 2, 'Chef de service', 'SERVICE_HEAD', 2, 'WORKING_DAYS', 'Valider le constat', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001513', '-', '')), @t, 3, 'Directeur', 'DIRECTOR', 2, 'WORKING_DAYS', 'Viser', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001514', '-', '')), @t, 4, 'Commission réception', 'DIRECTOR', 5, 'WORKING_DAYS', 'Tenir la commission', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001515', '-', '')), @t, 5, 'Clôture', 'SUPPORT', 2, 'WORKING_DAYS', 'Archiver le PV', FALSE, TRUE, NOW(6), NOW(6));

-- ENT-PROG
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000036', '-', '')),
       'ENT-PROG', 'Entretien routier programmé', 'Scheduled road maintenance',
       'Programme d''entretien routier.', 'DGTI-DEP', 71, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'ENT-PROG');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000001601', '-', '')),
       'T-ENT-PROG', 'Entretien programmé', 'Service → direction → programmation → DG → lancement (15 j.o.).',
       'ENT-PROG', 15, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-ENT-PROG');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-ENT-PROG' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001611', '-', '')), @t, 1, 'Service entretien', 'AGENT', 3, 'WORKING_DAYS', 'Préparer le programme', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001612', '-', '')), @t, 2, 'Direction entretien', 'DIRECTOR', 3, 'WORKING_DAYS', 'Valider', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001613', '-', '')), @t, 3, 'Programmation', 'SERVICE_HEAD', 5, 'WORKING_DAYS', 'Inscrire au programme', TRUE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001614', '-', '')), @t, 4, 'Validation DG', 'DIRECTOR', 2, 'WORKING_DAYS', 'Viser', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001615', '-', '')), @t, 5, 'Lancement', 'SERVICE_HEAD', 2, 'WORKING_DAYS', 'Lancer / clôturer', FALSE, TRUE, NOW(6), NOW(6));

-- INSP-CHANT (EXTERNAL)
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000037', '-', '')),
       'INSP-CHANT', 'Demande d''inspection chantier', 'Site inspection request',
       'Demande d''inspection / visite de chantier.', 'DRTP-C', 72, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'INSP-CHANT');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000001701', '-', '')),
       'T-INSP-CHANT', 'Inspection chantier', 'Dépôt → planification → visite → rapport → visa (10 j.o.).',
       'INSP-CHANT', 10, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-INSP-CHANT');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-INSP-CHANT' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001711', '-', '')), @t, 1, 'Réception', 'SUPPORT', 1, 'WORKING_DAYS', 'Enregistrer', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001712', '-', '')), @t, 2, 'Planification visite', 'SERVICE_HEAD', 3, 'WORKING_DAYS', 'Planifier', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001713', '-', '')), @t, 3, 'Visite', 'AGENT', 1, 'WORKING_DAYS', 'Effectuer la visite', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001714', '-', '')), @t, 4, 'Rapport', 'AGENT', 3, 'WORKING_DAYS', 'Rédiger le rapport', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001715', '-', '')), @t, 5, 'Visa chef service', 'SERVICE_HEAD', 2, 'WORKING_DAYS', 'Viser et clôturer', FALSE, TRUE, NOW(6), NOW(6));

INSERT INTO preconfigured_dossiers (id, code, name, name_en, description, file_type_code, chain_template_id, direction_code, sort_order, active, portal_enabled, portal_audience, form_schema, required_attachment_keys, created_at, updated_at)
SELECT UNHEX(REPLACE('f1000000-0000-4000-8000-000000000037', '-', '')),
       'INSP-CHANT', 'Demande d''inspection chantier', 'Site inspection request',
       'Portail externe — inspection chantier.', 'INSP-CHANT', @t, 'DRTP-C', 72, TRUE, TRUE, 'EXTERNAL',
       '{"fields":[{"key":"referenceMarche","label":"Réf. marché / chantier","type":"TEXT","required":true,"maxLength":80},{"key":"localisation","label":"Localisation","type":"TEXT","required":true,"maxLength":255},{"key":"motif","label":"Motif","type":"TEXT","required":true,"maxLength":500}]}',
       '[]', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM preconfigured_dossiers WHERE code = 'INSP-CHANT');

-- AUTH-OCC (EXTERNAL)
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000038', '-', '')),
       'AUTH-OCC', 'Autorisation occupation temporaire', 'Temporary occupation permit',
       'Occupation temporaire du domaine public.', 'DRTP-C', 41, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'AUTH-OCC');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000001801', '-', '')),
       'T-AUTH-OCC', 'Occupation temporaire', 'Réception → instruction → avis → directeur → acte (13 j.o.).',
       'AUTH-OCC', 13, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-AUTH-OCC');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-AUTH-OCC' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001811', '-', '')), @t, 1, 'Réception', 'SUPPORT', 1, 'WORKING_DAYS', 'Enregistrer', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001812', '-', '')), @t, 2, 'Instruction', 'AGENT', 4, 'WORKING_DAYS', 'Instruire', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001813', '-', '')), @t, 3, 'Avis technique', 'SERVICE_HEAD', 3, 'WORKING_DAYS', 'Émettre l''avis', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001814', '-', '')), @t, 4, 'Directeur DRTP', 'REGIONAL_DIRECTOR', 3, 'WORKING_DAYS', 'Autoriser', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001815', '-', '')), @t, 5, 'Délivrance acte', 'SUPPORT', 2, 'WORKING_DAYS', 'Délivrer', FALSE, TRUE, NOW(6), NOW(6));

INSERT INTO preconfigured_dossiers (id, code, name, name_en, description, file_type_code, chain_template_id, direction_code, sort_order, active, portal_enabled, portal_audience, form_schema, required_attachment_keys, created_at, updated_at)
SELECT UNHEX(REPLACE('f1000000-0000-4000-8000-000000000038', '-', '')),
       'AUTH-OCC', 'Autorisation occupation temporaire', 'Temporary occupation permit',
       'Portail externe — occupation temporaire.', 'AUTH-OCC', @t, 'DRTP-C', 41, TRUE, TRUE, 'EXTERNAL',
       '{"fields":[{"key":"demandeur","label":"Demandeur","type":"TEXT","required":true,"maxLength":255},{"key":"emprise","label":"Emprise / localisation","type":"TEXT","required":true,"maxLength":255},{"key":"dateDebut","label":"Début","type":"DATE","required":true},{"key":"dateFin","label":"Fin","type":"DATE","required":true}]}',
       '["plans"]', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM preconfigured_dossiers WHERE code = 'AUTH-OCC');

-- DECL-CHANT (EXTERNAL)
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000039', '-', '')),
       'DECL-CHANT', 'Déclaration ouverture / reprise chantier', 'Site opening declaration',
       'Déclaration d''ouverture ou reprise de chantier.', 'DRTP-C', 42, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'DECL-CHANT');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000001901', '-', '')),
       'T-DECL-CHANT', 'Déclaration chantier', 'Dépôt → vérif → visa → accusé (6 j.o.).',
       'DECL-CHANT', 6, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-DECL-CHANT');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-DECL-CHANT' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001911', '-', '')), @t, 1, 'Dépôt', 'SUPPORT', 1, 'WORKING_DAYS', 'Enregistrer', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001912', '-', '')), @t, 2, 'Vérification pièces', 'AGENT', 2, 'WORKING_DAYS', 'Contrôler', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001913', '-', '')), @t, 3, 'Visa service', 'SERVICE_HEAD', 2, 'WORKING_DAYS', 'Viser', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000001914', '-', '')), @t, 4, 'Accusé / suivi', 'SUPPORT', 1, 'WORKING_DAYS', 'Accuser réception', FALSE, TRUE, NOW(6), NOW(6));

INSERT INTO preconfigured_dossiers (id, code, name, name_en, description, file_type_code, chain_template_id, direction_code, sort_order, active, portal_enabled, portal_audience, form_schema, required_attachment_keys, created_at, updated_at)
SELECT UNHEX(REPLACE('f1000000-0000-4000-8000-000000000039', '-', '')),
       'DECL-CHANT', 'Déclaration ouverture / reprise chantier', 'Site opening declaration',
       'Portail externe — déclaration chantier.', 'DECL-CHANT', @t, 'DRTP-C', 42, TRUE, TRUE, 'EXTERNAL',
       '{"fields":[{"key":"referenceMarche","label":"Réf. marché","type":"TEXT","required":true,"maxLength":80},{"key":"typeDeclaration","label":"Type","type":"ENUM","required":true,"options":["OUVERTURE","REPRISE"]},{"key":"datePrevue","label":"Date prévue","type":"DATE","required":true}]}',
       '[]', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM preconfigured_dossiers WHERE code = 'DECL-CHANT');

SELECT 'batch-a-done' AS status;
