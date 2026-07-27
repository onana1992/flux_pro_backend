-- Objectif : reste circuits batch B (OA, construction, études, RH, contentieux, coop)
-- Suite de 2026-07-25_seed_remaining_circuits_a.sql
-- Idempotent

-- OA-SUIVI
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000040', '-', '')),
       'OA-SUIVI', 'Suivi ouvrage d''art', 'Bridge/structure follow-up',
       'Suivi construction / réhabilitation OA.', 'DGTI-DOA', 80, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'OA-SUIVI');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000002001', '-', '')),
       'T-OA-SUIVI', 'Suivi OA', 'Cellule → chef DOA → DG → SG? (13 j.o.).',
       'OA-SUIVI', 13, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-OA-SUIVI');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-OA-SUIVI' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002011', '-', '')), @t, 1, 'Cellule DOA', 'AGENT', 4, 'WORKING_DAYS', 'Instruire le suivi', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002012', '-', '')), @t, 2, 'Chef Division DOA', 'SERVICE_HEAD', 3, 'WORKING_DAYS', 'Valider', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002013', '-', '')), @t, 3, 'Direction / DG', 'DIRECTOR', 3, 'WORKING_DAYS', 'Arbitrer', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002014', '-', '')), @t, 4, 'SG (si seuil)', 'SECRETARY_GENERAL', 3, 'WORKING_DAYS', 'Viser si requis', TRUE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002015', '-', '')), @t, 5, 'Clôture', 'SUPPORT', 0, 'WORKING_DAYS', 'Clôturer', FALSE, TRUE, NOW(6), NOW(6));

-- OA-INSP
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000041', '-', '')),
       'OA-INSP', 'Inspection / surveillance OA', 'Structure inspection',
       'Inspection et surveillance d''ouvrage d''art.', 'DGTI-DOA', 81, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'OA-INSP');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000002101', '-', '')),
       'T-OA-INSP', 'Inspection OA', 'Programmation → inspection → rapport → chef → décision (12 j.o.).',
       'OA-INSP', 12, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-OA-INSP');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-OA-INSP' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002111', '-', '')), @t, 1, 'Programmation', 'SERVICE_HEAD', 2, 'WORKING_DAYS', 'Programmer', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002112', '-', '')), @t, 2, 'Inspection', 'AGENT', 2, 'WORKING_DAYS', 'Inspecter', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002113', '-', '')), @t, 3, 'Rapport cellule', 'AGENT', 3, 'WORKING_DAYS', 'Rédiger', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002114', '-', '')), @t, 4, 'Chef DOA', 'SERVICE_HEAD', 2, 'WORKING_DAYS', 'Valider', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002115', '-', '')), @t, 5, 'Décision entretien', 'DIRECTOR', 3, 'WORKING_DAYS', 'Décider', FALSE, TRUE, NOW(6), NOW(6));

-- REGIE
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000042', '-', '')),
       'REGIE', 'Travaux en régie', 'Force-account works',
       'Travaux en régie — brigade nationale.', 'DGTI-BNR', 82, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'REGIE');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000002201', '-', '')),
       'T-REGIE', 'Travaux en régie', 'Proposition → chef brigade → DG → lancement → CR (14 j.o.).',
       'REGIE', 14, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-REGIE');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-REGIE' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002211', '-', '')), @t, 1, 'Proposition section', 'AGENT', 3, 'WORKING_DAYS', 'Proposer', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002212', '-', '')), @t, 2, 'Chef brigade', 'SERVICE_HEAD', 3, 'WORKING_DAYS', 'Valider', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002213', '-', '')), @t, 3, 'DG DGTI', 'DIRECTOR', 3, 'WORKING_DAYS', 'Autoriser', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002214', '-', '')), @t, 4, 'Lancement', 'SERVICE_HEAD', 2, 'WORKING_DAYS', 'Lancer', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002215', '-', '')), @t, 5, 'Compte rendu', 'AGENT', 3, 'WORKING_DAYS', 'Rendre compte', FALSE, TRUE, NOW(6), NOW(6));

-- CONST-BAT
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000043', '-', '')),
       'CONST-BAT', 'Construction bâtiment public', 'Public building construction',
       'Dossier construction / reconstruction bâtiment public.', 'DGTI-DCO', 83, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'CONST-BAT');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000002301', '-', '')),
       'T-CONST-BAT', 'Construction bâtiment', 'Direction → visas tech → DG → SG → notification (18 j.o.).',
       'CONST-BAT', 18, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-CONST-BAT');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-CONST-BAT' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002311', '-', '')), @t, 1, 'Direction Construction', 'AGENT', 5, 'WORKING_DAYS', 'Instruire', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002312', '-', '')), @t, 2, 'Visas techniques', 'SERVICE_HEAD', 5, 'WORKING_DAYS', 'Viser techniquement', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002313', '-', '')), @t, 3, 'DG', 'DIRECTOR', 3, 'WORKING_DAYS', 'Valider', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002314', '-', '')), @t, 4, 'SG', 'SECRETARY_GENERAL', 3, 'WORKING_DAYS', 'Viser', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002315', '-', '')), @t, 5, 'Notification', 'SUPPORT', 2, 'WORKING_DAYS', 'Notifier', FALSE, TRUE, NOW(6), NOW(6));

-- AVIS-TECH (EXTERNAL)
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000044', '-', '')),
       'AVIS-TECH', 'Demande d''avis technique', 'Technical opinion request',
       'Demande d''avis / étude technique DGET.', 'DGET', 90, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'AVIS-TECH');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000002401', '-', '')),
       'T-AVIS-TECH', 'Avis technique', 'Réception → études → DPPN? → DG → transmission (15 j.o.).',
       'AVIS-TECH', 15, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-AVIS-TECH');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-AVIS-TECH' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002411', '-', '')), @t, 1, 'Réception DGET', 'SUPPORT', 2, 'WORKING_DAYS', 'Enregistrer', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002412', '-', '')), @t, 2, 'Direction études', 'AGENT', 5, 'WORKING_DAYS', 'Étudier', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002413', '-', '')), @t, 3, 'DPPN (si norme)', 'SERVICE_HEAD', 3, 'WORKING_DAYS', 'Avis normes', TRUE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002414', '-', '')), @t, 4, 'DG DGET', 'DIRECTOR', 3, 'WORKING_DAYS', 'Valider l''avis', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002415', '-', '')), @t, 5, 'Transmission avis', 'SUPPORT', 2, 'WORKING_DAYS', 'Transmettre', FALSE, TRUE, NOW(6), NOW(6));

INSERT INTO preconfigured_dossiers (id, code, name, name_en, description, file_type_code, chain_template_id, direction_code, sort_order, active, portal_enabled, portal_audience, form_schema, required_attachment_keys, created_at, updated_at)
SELECT UNHEX(REPLACE('f1000000-0000-4000-8000-000000000044', '-', '')),
       'AVIS-TECH', 'Demande d''avis technique', 'Technical opinion request',
       'Portail externe — avis technique.', 'AVIS-TECH', @t, 'DGET', 90, TRUE, TRUE, 'EXTERNAL',
       '{"fields":[{"key":"objet","label":"Objet de la demande","type":"TEXT","required":true,"maxLength":255},{"key":"contexte","label":"Contexte","type":"TEXT","required":true,"maxLength":1000},{"key":"delaiSouhaite","label":"Délai souhaité","type":"DATE","required":false}]}',
       '["pieces"]', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM preconfigured_dossiers WHERE code = 'AVIS-TECH');

-- NORME
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000045', '-', '')),
       'NORME', 'Élaboration / MAJ norme technique', 'Technical standard update',
       'Élaboration ou mise à jour de norme.', 'DGET-DPPN', 91, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'NORME');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000002501', '-', '')),
       'T-NORME', 'Norme technique', 'Normalisation → DPPN → DG → SG → diffusion (27 j.o.).',
       'NORME', 27, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-NORME');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-NORME' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002511', '-', '')), @t, 1, 'Cellule Normalisation', 'AGENT', 10, 'WORKING_DAYS', 'Rédiger', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002512', '-', '')), @t, 2, 'DPPN', 'SERVICE_HEAD', 5, 'WORKING_DAYS', 'Valider', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002513', '-', '')), @t, 3, 'DG DGET', 'DIRECTOR', 5, 'WORKING_DAYS', 'Approuver', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002514', '-', '')), @t, 4, 'Validation SG', 'SECRETARY_GENERAL', 5, 'WORKING_DAYS', 'Viser', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002515', '-', '')), @t, 5, 'Diffusion', 'SUPPORT', 2, 'WORKING_DAYS', 'Diffuser', FALSE, TRUE, NOW(6), NOW(6));

-- PROG-INFRA
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000046', '-', '')),
       'PROG-INFRA', 'Programmation / plan directeur', 'Infrastructure programming',
       'Fiche de programmation / plan directeur.', 'DGET-DPPN', 92, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'PROG-INFRA');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000002601', '-', '')),
       'T-PROG-INFRA', 'Programmation infra', 'Programmation → DPPN → DG → concertation? → adoption (28 j.o.).',
       'PROG-INFRA', 28, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-PROG-INFRA');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-PROG-INFRA' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002611', '-', '')), @t, 1, 'Cellule Programmation', 'AGENT', 5, 'WORKING_DAYS', 'Élaborer', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002612', '-', '')), @t, 2, 'DPPN', 'SERVICE_HEAD', 5, 'WORKING_DAYS', 'Valider', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002613', '-', '')), @t, 3, 'DG', 'DIRECTOR', 3, 'WORKING_DAYS', 'Approuver', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002614', '-', '')), @t, 4, 'Concertation externe', 'SECRETARY_GENERAL', 10, 'WORKING_DAYS', 'Concerter si requis', TRUE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002615', '-', '')), @t, 5, 'Adoption', 'EXECUTIVE_OFFICE', 5, 'WORKING_DAYS', 'Adopter', FALSE, TRUE, NOW(6), NOW(6));

-- MISSION (INTERNAL)
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000047', '-', '')),
       'MISSION', 'Ordre de mission', 'Mission order',
       'Demande d''ordre de mission.', 'DAG', 61, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'MISSION');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000002701', '-', '')),
       'T-MISSION', 'Ordre de mission', 'Demandeur → chef → directeur → moyens → édition (7 j.o.).',
       'MISSION', 7, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-MISSION');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-MISSION' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002711', '-', '')), @t, 1, 'Réception RH', 'SUPPORT', 1, 'WORKING_DAYS', 'Enregistrer', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002712', '-', '')), @t, 2, 'Chef de service', 'SERVICE_HEAD', 1, 'WORKING_DAYS', 'Viser', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002713', '-', '')), @t, 3, 'Directeur', 'DIRECTOR', 2, 'WORKING_DAYS', 'Autoriser', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002714', '-', '')), @t, 4, 'Moyens / engagement', 'SERVICE_HEAD', 2, 'WORKING_DAYS', 'Engager', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002715', '-', '')), @t, 5, 'Édition OM', 'SUPPORT', 1, 'WORKING_DAYS', 'Éditer', FALSE, TRUE, NOW(6), NOW(6));

INSERT INTO preconfigured_dossiers (id, code, name, name_en, description, file_type_code, chain_template_id, direction_code, sort_order, active, portal_enabled, portal_audience, form_schema, required_attachment_keys, created_at, updated_at)
SELECT UNHEX(REPLACE('f1000000-0000-4000-8000-000000000047', '-', '')),
       'MISSION', 'Ordre de mission', 'Mission order',
       'Portail interne — ordre de mission.', 'MISSION', @t, 'DAG', 61, TRUE, TRUE, 'INTERNAL',
       '{"fields":[{"key":"destination","label":"Destination","type":"TEXT","required":true,"maxLength":255},{"key":"dateDebut","label":"Début","type":"DATE","required":true},{"key":"dateFin","label":"Fin","type":"DATE","required":true},{"key":"objet","label":"Objet de la mission","type":"TEXT","required":true,"maxLength":500}]}',
       '[]', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM preconfigured_dossiers WHERE code = 'MISSION');

-- ATT-SERV (INTERNAL)
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000048', '-', '')),
       'ATT-SERV', 'Attestation de service', 'Service certificate',
       'Demande d''attestation de service / travail.', 'DAG', 62, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'ATT-SERV');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000002801', '-', '')),
       'T-ATT-SERV', 'Attestation de service', 'Demande → RH → visa DAG → remise (5 j.o.).',
       'ATT-SERV', 5, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-ATT-SERV');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-ATT-SERV' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002811', '-', '')), @t, 1, 'Réception demande', 'SUPPORT', 1, 'WORKING_DAYS', 'Enregistrer', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002812', '-', '')), @t, 2, 'Vérification RH', 'SERVICE_HEAD', 2, 'WORKING_DAYS', 'Vérifier', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002813', '-', '')), @t, 3, 'Visa DAG', 'DIRECTOR', 1, 'WORKING_DAYS', 'Viser', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002814', '-', '')), @t, 4, 'Remise', 'SUPPORT', 1, 'WORKING_DAYS', 'Remettre', FALSE, TRUE, NOW(6), NOW(6));

INSERT INTO preconfigured_dossiers (id, code, name, name_en, description, file_type_code, chain_template_id, direction_code, sort_order, active, portal_enabled, portal_audience, form_schema, required_attachment_keys, created_at, updated_at)
SELECT UNHEX(REPLACE('f1000000-0000-4000-8000-000000000048', '-', '')),
       'ATT-SERV', 'Attestation de service', 'Service certificate',
       'Portail interne — attestation.', 'ATT-SERV', @t, 'DAG', 62, TRUE, TRUE, 'INTERNAL',
       '{"fields":[{"key":"typeAttestation","label":"Type","type":"ENUM","required":true,"options":["TRAVAIL","SERVICE","PRESENCE"]},{"key":"motif","label":"Motif","type":"TEXT","required":false,"maxLength":255}]}',
       '[]', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM preconfigured_dossiers WHERE code = 'ATT-SERV');

-- ACHAT-INT
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000049', '-', '')),
       'ACHAT-INT', 'Demande d''achat / engagement', 'Internal purchase request',
       'Demande d''achat / engagement budgétaire interne.', 'DAG', 63, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'ACHAT-INT');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000002901', '-', '')),
       'T-ACHAT-INT', 'Achat interne', 'Demandeur → moyens → budget → directeur → commande (12 j.o.).',
       'ACHAT-INT', 12, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-ACHAT-INT');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-ACHAT-INT' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002911', '-', '')), @t, 1, 'Service demandeur', 'AGENT', 2, 'WORKING_DAYS', 'Constituer', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002912', '-', '')), @t, 2, 'DAG / Moyens', 'SERVICE_HEAD', 3, 'WORKING_DAYS', 'Instruire', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002913', '-', '')), @t, 3, 'Visa budgétaire', 'SERVICE_HEAD', 3, 'WORKING_DAYS', 'Viser budget', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002914', '-', '')), @t, 4, 'Directeur', 'DIRECTOR', 2, 'WORKING_DAYS', 'Autoriser', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000002915', '-', '')), @t, 5, 'Commande', 'SUPPORT', 2, 'WORKING_DAYS', 'Passer commande', FALSE, TRUE, NOW(6), NOW(6));

-- RECLAM (EXTERNAL)
INSERT INTO file_types (id, code, name, name_en, description, direction_code, sort_order, active, created_at, updated_at)
SELECT UNHEX(REPLACE('e1000000-0000-4000-8000-000000000050', '-', '')),
       'RECLAM', 'Réclamation / contentieux', 'Claim / dispute',
       'Réclamation ou contentieux usager / entreprise.', 'DAG-JUR', 95, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM file_types WHERE code = 'RECLAM');

INSERT INTO chain_templates (id, code, name, description, file_type_code, total_delay_days, delay_unit, active, system_template, created_at, updated_at)
SELECT UNHEX(REPLACE('d1000000-0000-4000-8000-000000003001', '-', '')),
       'T-RECLAM', 'Réclamation', 'Réception → métier → juridique → directeur → SG → réponse (19 j.o.).',
       'RECLAM', 19, 'WORKING_DAYS', TRUE, FALSE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T-RECLAM');

SET @t := (SELECT id FROM chain_templates WHERE code = 'T-RECLAM' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000003011', '-', '')), @t, 1, 'Réception', 'SUPPORT', 1, 'WORKING_DAYS', 'Enregistrer', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000003012', '-', '')), @t, 2, 'Service métier', 'AGENT', 5, 'WORKING_DAYS', 'Instruire', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000003013', '-', '')), @t, 3, 'Affaires juridiques', 'AGENT', 5, 'WORKING_DAYS', 'Avis juridique', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000003014', '-', '')), @t, 4, 'Directeur', 'DIRECTOR', 3, 'WORKING_DAYS', 'Arbitrer', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000003015', '-', '')), @t, 5, 'SG', 'SECRETARY_GENERAL', 3, 'WORKING_DAYS', 'Valider', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000003016', '-', '')), @t, 6, 'Réponse', 'SUPPORT', 2, 'WORKING_DAYS', 'Notifier', FALSE, TRUE, NOW(6), NOW(6));

INSERT INTO preconfigured_dossiers (id, code, name, name_en, description, file_type_code, chain_template_id, direction_code, sort_order, active, portal_enabled, portal_audience, form_schema, required_attachment_keys, created_at, updated_at)
SELECT UNHEX(REPLACE('f1000000-0000-4000-8000-000000000050', '-', '')),
       'RECLAM', 'Réclamation / contentieux', 'Claim / dispute',
       'Portail externe — réclamation.', 'RECLAM', @t, 'DAG-JUR', 95, TRUE, TRUE, 'EXTERNAL',
       '{"fields":[{"key":"objet","label":"Objet","type":"TEXT","required":true,"maxLength":255},{"key":"faits","label":"Exposé des faits","type":"TEXT","required":true,"maxLength":2000},{"key":"referenceDossier","label":"Réf. dossier lié","type":"TEXT","required":false,"maxLength":80}]}',
       '["pieces"]', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM preconfigured_dossiers WHERE code = 'RECLAM');

-- COOP : activer COOP-PART / enrichir T05
UPDATE file_types
SET active = TRUE, name = 'Coopération / partenariat', description = 'Dossier coopération / partenariat / tutelle.', updated_at = NOW(6)
WHERE code = 'COOP-PART';

UPDATE chain_templates
SET active = TRUE,
    name = 'Coopération / partenariat',
    description = 'Direction → DG → SG → Cabinet → notification (20 j.o.).',
    total_delay_days = 20,
    updated_at = NOW(6)
WHERE code = 'T05';

SET @t := (SELECT id FROM chain_templates WHERE code = 'T05' LIMIT 1);
DELETE FROM chain_step_templates WHERE chain_template_id = @t;
INSERT INTO chain_step_templates (id, chain_template_id, step_order, label, responsible_role, delay_value, delay_unit, expected_action, optional, closure_step, created_at, updated_at) VALUES
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000511', '-', '')), @t, 1, 'Direction concernée', 'DIRECTOR', 5, 'WORKING_DAYS', 'Instruire', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000512', '-', '')), @t, 2, 'DG', 'DIRECTOR', 3, 'WORKING_DAYS', 'Valider', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000513', '-', '')), @t, 3, 'SG', 'SECRETARY_GENERAL', 5, 'WORKING_DAYS', 'Arbitrer', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000514', '-', '')), @t, 4, 'Cabinet', 'EXECUTIVE_OFFICE', 5, 'WORKING_DAYS', 'Décider', FALSE, FALSE, NOW(6), NOW(6)),
(UNHEX(REPLACE('d1000000-0000-4000-8000-000000000515', '-', '')), @t, 5, 'Notification partenaire', 'SUPPORT', 2, 'WORKING_DAYS', 'Notifier', FALSE, TRUE, NOW(6), NOW(6));

-- Synthèse
SELECT ft.code AS file_type, ct.code AS chain, pd.code AS preconfigured, pd.portal_audience
FROM file_types ft
LEFT JOIN chain_templates ct ON ct.file_type_code = ft.code AND ct.active = TRUE
LEFT JOIN preconfigured_dossiers pd ON pd.file_type_code = ft.code
ORDER BY ft.sort_order, ft.code, ct.code;
