-- Objectif : compléter l'organigramme MINTP (Phase A — ORGANIGRAMME-MINTP-CIRCUITS-FLUXPRO.md)
-- Tables : organization
-- Prérequis : MINTP + organization_type (MINISTRY/DIRECTORATE/DIVISION/SERVICE/REGIONAL_DIRECTORATE)
-- Exécution : manuelle MySQL/MariaDB — idempotent
-- Compatibilité : conserve DIER (alias pilote CDC) ; ajoute DGTI/DGET officiels
--
-- UUID org : c2000000-0000-4000-8000-0000000000xx

SET @t_dir  := '00000000-0000-4000-8000-000000000002'; -- DIRECTORATE
SET @t_div  := '00000000-0000-4000-8000-000000000003'; -- DIVISION
SET @t_svc  := '00000000-0000-4000-8000-000000000004'; -- SERVICE
SET @t_reg  := '00000000-0000-4000-8000-000000000005'; -- REGIONAL_DIRECTORATE

SET @mintp := (SELECT id FROM organization WHERE code = 'MINTP' LIMIT 1);

-- ═══════════════════════════════════════════════════════════════════════════
-- 1. Cabinet / SG / Inspection / cellules (sous MINTP)
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000001', '-', '')), 'MINTP-SG',
       'Secrétariat Général', @t_dir, @mintp, TRUE, NOW(6), NOW(6)
WHERE @mintp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'MINTP-SG');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000002', '-', '')), 'MINTP-IG',
       'Inspection Générale', @t_dir, @mintp, TRUE, NOW(6), NOW(6)
WHERE @mintp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'MINTP-IG');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000003', '-', '')), 'CEL-COM',
       'Cellule Communication', @t_svc, @mintp, TRUE, NOW(6), NOW(6)
WHERE @mintp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'CEL-COM');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000004', '-', '')), 'CEL-BIL',
       'Cellule Promotion du bilinguisme', @t_svc, @mintp, TRUE, NOW(6), NOW(6)
WHERE @mintp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'CEL-BIL');

-- Cabinet déjà présent (MINTP-CABINET) via seed antérieur — normaliser le libellé
UPDATE organization
SET name = 'Cabinet du Ministre', updated_at = NOW(6)
WHERE code = 'MINTP-CABINET';

UPDATE organization
SET name = 'Direction des Affaires Générales', updated_at = NOW(6)
WHERE code = 'DAG';

-- ═══════════════════════════════════════════════════════════════════════════
-- 2. DAG — services manquants
-- ═══════════════════════════════════════════════════════════════════════════

SET @dag := (SELECT id FROM organization WHERE code = 'DAG' LIMIT 1);

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000010', '-', '')), 'DAG-RH',
       'Sous-Direction RH / Personnel', @t_div, @dag, TRUE, NOW(6), NOW(6)
WHERE @dag IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DAG-RH');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000011', '-', '')), 'DAG-BUDGET',
       'Sous-Direction Budget / Moyens', @t_div, @dag, TRUE, NOW(6), NOW(6)
WHERE @dag IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DAG-BUDGET');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000012', '-', '')), 'DAG-MARCHES',
       'Sous-Direction des Marchés administratifs', @t_div, @dag, TRUE, NOW(6), NOW(6)
WHERE @dag IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DAG-MARCHES');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000013', '-', '')), 'DAG-JUR',
       'Division Affaires Juridiques', @t_div, @dag, TRUE, NOW(6), NOW(6)
WHERE @dag IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DAG-JUR');

-- ═══════════════════════════════════════════════════════════════════════════
-- 3. DGTI + directions / divisions (DIER conservé en parallèle)
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000020', '-', '')), 'DGTI',
       'Direction Générale des Travaux d''Infrastructures', @t_dir, @mintp, TRUE, NOW(6), NOW(6)
WHERE @mintp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DGTI');

SET @dgti := (SELECT id FROM organization WHERE code = 'DGTI' LIMIT 1);

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000021', '-', '')), 'DGTI-DIR',
       'Direction des Investissements Routiers', @t_dir, @dgti, TRUE, NOW(6), NOW(6)
WHERE @dgti IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DGTI-DIR');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000022', '-', '')), 'DGTI-DEP',
       'Direction de l''Entretien et de la Protection du Patrimoine Routier', @t_dir, @dgti, TRUE, NOW(6), NOW(6)
WHERE @dgti IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DGTI-DEP');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000023', '-', '')), 'DGTI-DRR',
       'Direction des Routes Rurales / Communales', @t_dir, @dgti, TRUE, NOW(6), NOW(6)
WHERE @dgti IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DGTI-DRR');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000024', '-', '')), 'DGTI-DCO',
       'Direction de la Construction', @t_dir, @dgti, TRUE, NOW(6), NOW(6)
WHERE @dgti IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DGTI-DCO');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000025', '-', '')), 'DGTI-DOA',
       'Division des Ouvrages d''Art', @t_div, @dgti, TRUE, NOW(6), NOW(6)
WHERE @dgti IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DGTI-DOA');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000026', '-', '')), 'DGTI-BNR',
       'Brigade nationale des travaux en régie', @t_div, @dgti, TRUE, NOW(6), NOW(6)
WHERE @dgti IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DGTI-BNR');

SET @doa := (SELECT id FROM organization WHERE code = 'DGTI-DOA' LIMIT 1);

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000027', '-', '')), 'DGTI-DOA-SURV',
       'Cellule gestion & surveillance OA', @t_svc, @doa, TRUE, NOW(6), NOW(6)
WHERE @doa IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DGTI-DOA-SURV');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000028', '-', '')), 'DGTI-DOA-CONST',
       'Cellule construction / entretien / réhabilitation OA', @t_svc, @doa, TRUE, NOW(6), NOW(6)
WHERE @doa IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DGTI-DOA-CONST');

-- Note descriptive sur DIER (alias CDC)
UPDATE organization
SET
    name = 'Direction des Investissements et de l''Entretien Routier (alias pilote CDC)',
    updated_at = NOW(6)
WHERE code = 'DIER';

-- ═══════════════════════════════════════════════════════════════════════════
-- 4. DGET + DPPN + études
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000030', '-', '')), 'DGET',
       'Direction Générale des Études Techniques', @t_dir, @mintp, TRUE, NOW(6), NOW(6)
WHERE @mintp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DGET');

SET @dget := (SELECT id FROM organization WHERE code = 'DGET' LIMIT 1);

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000031', '-', '')), 'DGET-DPPN',
       'Division Planification, Programmation et Normes', @t_div, @dget, TRUE, NOW(6), NOW(6)
WHERE @dget IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DGET-DPPN');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000032', '-', '')), 'DGET-DETROA',
       'Direction des Études Techniques Routières et d''Ouvrages d''Art', @t_dir, @dget, TRUE, NOW(6), NOW(6)
WHERE @dget IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DGET-DETROA');

SET @dppn := (SELECT id FROM organization WHERE code = 'DGET-DPPN' LIMIT 1);

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000033', '-', '')), 'DGET-DPPN-PLAN',
       'Cellule Planification', @t_svc, @dppn, TRUE, NOW(6), NOW(6)
WHERE @dppn IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DGET-DPPN-PLAN');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000034', '-', '')), 'DGET-DPPN-NORM',
       'Cellule Normalisation technique', @t_svc, @dppn, TRUE, NOW(6), NOW(6)
WHERE @dppn IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DGET-DPPN-NORM');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000035', '-', '')), 'DGET-DPPN-PROG',
       'Cellule Programmation', @t_svc, @dppn, TRUE, NOW(6), NOW(6)
WHERE @dppn IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DGET-DPPN-PROG');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000036', '-', '')), 'DGET-DPPN-SUIV',
       'Cellule Suivi', @t_svc, @dppn, TRUE, NOW(6), NOW(6)
WHERE @dppn IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DGET-DPPN-SUIV');

-- ═══════════════════════════════════════════════════════════════════════════
-- 5. DRTP Centre — services complémentaires
-- ═══════════════════════════════════════════════════════════════════════════

SET @drtp_c := (SELECT id FROM organization WHERE code = 'DRTP-C' LIMIT 1);

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000040', '-', '')), 'DRTP-C-ADMIN',
       'Service Administratif & Courrier', @t_svc, @drtp_c, TRUE, NOW(6), NOW(6)
WHERE @drtp_c IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DRTP-C-ADMIN');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000041', '-', '')), 'DRTP-C-ENT',
       'Service Entretien', @t_svc, @drtp_c, TRUE, NOW(6), NOW(6)
WHERE @drtp_c IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DRTP-C-ENT');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c2000000-0000-4000-8000-000000000042', '-', '')), 'DRTP-C-CTRL',
       'Service Contrôle / Suivi chantiers', @t_svc, @drtp_c, TRUE, NOW(6), NOW(6)
WHERE @drtp_c IS NOT NULL AND NOT EXISTS (SELECT 1 FROM organization WHERE code = 'DRTP-C-CTRL');

-- ═══════════════════════════════════════════════════════════════════════════
-- Vérification
-- ═══════════════════════════════════════════════════════════════════════════

SELECT o.code, o.name, p.code AS parent, ot.code AS type
FROM organization o
LEFT JOIN organization p ON p.id = o.parent_id
JOIN organization_type ot ON ot.id = o.organization_type_id
ORDER BY COALESCE(p.code, ''), o.code;
