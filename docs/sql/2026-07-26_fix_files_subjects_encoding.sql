-- Objectif : corriger objets / motifs / actions avec caractères corrompus (mojibake)
-- Tables : files, chain_step_templates
-- Exécution (Windows) : cmd /c "mysql -u … --default-character-set=utf8mb4 fluxpro < ce_fichier.sql"

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ═══════════════════════════════════════════════════════════════════════════
-- 1. Objets / expéditeurs / motifs des dossiers seed (a210…)
-- ═══════════════════════════════════════════════════════════════════════════

UPDATE files SET
    subject = 'Demande de congé annuel - exercice 2026',
    updated_at = NOW(6)
WHERE id = UNHEX(REPLACE('a2100000-0000-4000-8000-000000000001', '-', ''))
   OR (file_type_code = 'RH-CONGE' AND subject LIKE 'Demande de cong%');

UPDATE files SET
    subject = 'Effondrement chaussée RN3 PK 47 - intervention urgente',
    updated_at = NOW(6)
WHERE id = UNHEX(REPLACE('a2100000-0000-4000-8000-000000000002', '-', ''));

UPDATE files SET
    subject = 'Décompte n°3 - marché entretien RN1 tronçon Soa',
    sender_or_beneficiary = 'Entreprise Routière du Centre',
    updated_at = NOW(6)
WHERE id = UNHEX(REPLACE('a2100000-0000-4000-8000-000000000003', '-', ''));

UPDATE files SET
    subject = 'Autorisation travaux - pose fibre optique Avenue Kennedy',
    updated_at = NOW(6)
WHERE id = UNHEX(REPLACE('a2100000-0000-4000-8000-000000000004', '-', ''));

UPDATE files SET
    subject = 'Ordre de mission - supervision chantier RN4 Ebolowa',
    closure_reason = 'Mission exécutée - rapport de supervision archivé',
    updated_at = NOW(6)
WHERE id = UNHEX(REPLACE('a2100000-0000-4000-8000-000000000005', '-', ''));

-- ═══════════════════════════════════════════════════════════════════════════
-- 2. Actions de maillons : É mal encodé (├ë)
-- ═══════════════════════════════════════════════════════════════════════════

UPDATE chain_step_templates
SET
    expected_action = REPLACE(REPLACE(REPLACE(REPLACE(expected_action,
        '├ë', 'É'),
        '├©', 'é'),
        '├¿', 'è'),
        '├á', 'à'),
    updated_at = NOW(6)
WHERE expected_action LIKE '%├%';

UPDATE chain_step_templates
SET
    label = REPLACE(REPLACE(REPLACE(REPLACE(label,
        '├ë', 'É'),
        '├©', 'é'),
        '├¿', 'è'),
        '├á', 'à'),
    updated_at = NOW(6)
WHERE label LIKE '%├%';

-- Corrections canoniques ciblées (Émettre / Étudier)
UPDATE chain_step_templates s
JOIN chain_templates ct ON ct.id = s.chain_template_id
SET s.expected_action = 'Émettre l''avis',
    s.updated_at = NOW(6)
WHERE ct.code = 'T-AUTH-OCC'
  AND (s.expected_action LIKE '%mettre l%avis%' OR s.expected_action LIKE '├ëmettre%');

UPDATE chain_step_templates s
JOIN chain_templates ct ON ct.id = s.chain_template_id
SET s.expected_action = 'Étudier',
    s.updated_at = NOW(6)
WHERE ct.code = 'T-AVIS-TECH'
  AND (s.expected_action LIKE '%tudier' OR s.expected_action = '├ëtudier');

UPDATE chain_step_templates s
JOIN chain_templates ct ON ct.id = s.chain_template_id
SET s.expected_action = 'Élaborer',
    s.updated_at = NOW(6)
WHERE ct.code = 'T-PROG-INFRA'
  AND LOWER(s.expected_action) IN ('élaborer', 'elaborer');

-- ═══════════════════════════════════════════════════════════════════════════
-- 3. Vérification
-- ═══════════════════════════════════════════════════════════════════════════

SELECT
    reference_number,
    file_type_code,
    subject,
    sender_or_beneficiary,
    LEFT(IFNULL(closure_reason, ''), 80) AS closure_reason
FROM files
WHERE id IN (
    UNHEX(REPLACE('a2100000-0000-4000-8000-000000000001', '-', '')),
    UNHEX(REPLACE('a2100000-0000-4000-8000-000000000002', '-', '')),
    UNHEX(REPLACE('a2100000-0000-4000-8000-000000000003', '-', '')),
    UNHEX(REPLACE('a2100000-0000-4000-8000-000000000004', '-', '')),
    UNHEX(REPLACE('a2100000-0000-4000-8000-000000000005', '-', ''))
)
ORDER BY file_type_code;

SELECT ct.code, s.label, s.expected_action
FROM chain_step_templates s
JOIN chain_templates ct ON ct.id = s.chain_template_id
WHERE s.expected_action LIKE '%├%'
   OR s.label LIKE '%├%'
   OR s.expected_action LIKE '%??%'
   OR s.label LIKE '%??%';

SELECT reference_number, subject
FROM files
WHERE subject LIKE '%??%'
   OR sender_or_beneficiary LIKE '%??%'
   OR IFNULL(closure_reason, '') LIKE '%??%';
