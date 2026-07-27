-- Objectif : 5 dossiers de démonstration liés aux préconfigurés (circuits initialisés)
-- Tables : files, file_passages, file_number_sequences
-- Prérequis : preconfigured_dossiers + step_assignments + organizations + users
-- Idempotent : purge puis ré-insère les UUID seed a210…0001..0005 / f210…
-- Profil :
--   1. RH-CONGE   DRAFT
--   2. ENT-URG    IN_PROGRESS (VERY_URGENT) — 1er stage actif
--   3. DECOMPTE   IN_PROGRESS — stage 1 terminé, stage 2 actif
--   4. AUTH-TRAV  IN_PROGRESS (URGENT) — 1er stage actif
--   5. MISSION    CLOSED

SET NAMES utf8mb4;

SET @u_fallback := (SELECT id FROM users WHERE email = 'e.fotso@mintp.cm' LIMIT 1);

SET @f1 := UNHEX(REPLACE('a2100000-0000-4000-8000-000000000001', '-', ''));
SET @f2 := UNHEX(REPLACE('a2100000-0000-4000-8000-000000000002', '-', ''));
SET @f3 := UNHEX(REPLACE('a2100000-0000-4000-8000-000000000003', '-', ''));
SET @f4 := UNHEX(REPLACE('a2100000-0000-4000-8000-000000000004', '-', ''));
SET @f5 := UNHEX(REPLACE('a2100000-0000-4000-8000-000000000005', '-', ''));

-- ── 0. Purge seed précédent ──
DELETE FROM file_passage_cc
WHERE file_passage_id IN (
    SELECT id FROM (
        SELECT id FROM file_passages WHERE file_id IN (@f1, @f2, @f3, @f4, @f5)
    ) x
);

DELETE FROM file_passages WHERE file_id IN (@f1, @f2, @f3, @f4, @f5);
DELETE FROM files WHERE id IN (@f1, @f2, @f3, @f4, @f5);

-- Helper : UUID string depuis BINARY(16)
-- LOWER(CONCAT(SUBSTR(HEX(id),1,8),'-',SUBSTR(HEX(id),9,4),'-',SUBSTR(HEX(id),13,4),'-',SUBSTR(HEX(id),17,4),'-',SUBSTR(HEX(id),21,12)))

-- ── 1. RH-CONGE — brouillon ──
SET @pd := (SELECT id FROM preconfigured_dossiers WHERE code = 'RH-CONGE' LIMIT 1);
SET @org := (SELECT id FROM organization WHERE code = 'DAG' LIMIT 1);
SET @creator := COALESCE(
    (SELECT u.id FROM users u WHERE u.organization_id = @org AND u.active = 1 ORDER BY u.created_at LIMIT 1),
    @u_fallback
);

INSERT INTO files (
    id, reference_number, file_type_code, preconfigured_dossier_id, chain_template_id,
    organization_id, created_by_user_id, portal_user_id,
    subject, sender_or_beneficiary, received_at, priority, status,
    closure_reason, closed_at, cancellation_reason, cancelled_at,
    external_hold_reason, external_hold_since, metadata, created_at, updated_at
) VALUES (
    @f1, NULL, 'RH-CONGE', @pd, NULL,
    @org, @creator, NULL,
    'Demande de congé annuel - exercice 2026',
    'Agent DAG RH',
    '2026-07-20', 'NORMAL', 'DRAFT',
    NULL, NULL, NULL, NULL, NULL, NULL,
    JSON_OBJECT('joursDemandes', 10, 'motif', 'Congé annuel'),
    NOW(6), NOW(6)
);

-- ── 2. ENT-URG — en cours, urgence, circuit initialisé ──
SET @pd := (SELECT id FROM preconfigured_dossiers WHERE code = 'ENT-URG' LIMIT 1);
SET @ct := (SELECT chain_template_id FROM preconfigured_dossiers WHERE code = 'ENT-URG' LIMIT 1);
SET @org := (SELECT id FROM organization WHERE code = 'DRTP-C' LIMIT 1);
SET @creator := COALESCE(
    (SELECT u.id FROM users u WHERE u.organization_id = @org AND u.active = 1 ORDER BY u.created_at LIMIT 1),
    @u_fallback
);

INSERT INTO files (
    id, reference_number, file_type_code, preconfigured_dossier_id, chain_template_id,
    organization_id, created_by_user_id, portal_user_id,
    subject, sender_or_beneficiary, received_at, priority, status,
    closure_reason, closed_at, cancellation_reason, cancelled_at,
    external_hold_reason, external_hold_since, metadata, created_at, updated_at
) VALUES (
    @f2, 'MINTP-DRTP-C-2026-0101', 'ENT-URG', @pd, @ct,
    @org, @creator, NULL,
    'Effondrement chaussée RN3 PK 47 - intervention urgente',
    'Commune de Mbalmayo',
    '2026-07-22', 'VERY_URGENT', 'IN_PROGRESS',
    NULL, NULL, NULL, NULL, NULL, NULL,
    JSON_OBJECT('localisation', 'RN3 PK47', 'gravite', 'haute'),
    NOW(6), NOW(6)
);

INSERT INTO file_passages (
    id, file_id, chain_step_template_id, step_order, responsible_user_id,
    status, received_at, transmitted_at, due_at, created_at, updated_at
)
SELECT
    UNHEX(REPLACE(UUID(), '-', '')),
    @f2,
    s.id,
    s.step_order,
    UNHEX(REPLACE(JSON_UNQUOTE(JSON_EXTRACT(
        pd.step_assignments,
        CONCAT(
            '$."',
            LOWER(CONCAT(
                SUBSTR(HEX(s.id), 1, 8), '-',
                SUBSTR(HEX(s.id), 9, 4), '-',
                SUBSTR(HEX(s.id), 13, 4), '-',
                SUBSTR(HEX(s.id), 17, 4), '-',
                SUBSTR(HEX(s.id), 21, 12)
            )),
            '"'
        )
    )), '-', '')),
    CASE WHEN s.step_order = m.min_order THEN 'IN_PROGRESS' ELSE 'PENDING' END,
    CASE WHEN s.step_order = m.min_order THEN '2026-07-22 08:30:00.000000' ELSE NULL END,
    NULL,
    CASE
        WHEN s.step_order = m.min_order
        THEN DATE_ADD('2026-07-22 08:30:00', INTERVAL s.delay_value DAY)
        ELSE NULL
    END,
    NOW(6), NOW(6)
FROM preconfigured_dossiers pd
JOIN chain_step_templates s ON s.chain_template_id = pd.chain_template_id
JOIN (
    SELECT chain_template_id, MIN(step_order) AS min_order
    FROM chain_step_templates
    GROUP BY chain_template_id
) m ON m.chain_template_id = pd.chain_template_id
WHERE pd.code = 'ENT-URG';

-- ── 3. DECOMPTE — en cours, avancé au stage 2 ──
SET @pd := (SELECT id FROM preconfigured_dossiers WHERE code = 'DECOMPTE' LIMIT 1);
SET @ct := (SELECT chain_template_id FROM preconfigured_dossiers WHERE code = 'DECOMPTE' LIMIT 1);
SET @org := (SELECT id FROM organization WHERE code = 'DIER' LIMIT 1);
SET @creator := COALESCE(
    (SELECT u.id FROM users u WHERE u.organization_id = @org AND u.active = 1 ORDER BY u.created_at LIMIT 1),
    @u_fallback
);

INSERT INTO files (
    id, reference_number, file_type_code, preconfigured_dossier_id, chain_template_id,
    organization_id, created_by_user_id, portal_user_id,
    subject, sender_or_beneficiary, received_at, priority, status,
    closure_reason, closed_at, cancellation_reason, cancelled_at,
    external_hold_reason, external_hold_since, metadata, created_at, updated_at
) VALUES (
    @f3, 'MINTP-DIER-2026-0101', 'DECOMPTE', @pd, @ct,
    @org, @creator, NULL,
    'Décompte n°3 - marché entretien RN1 tronçon Soa',
    'Entreprise Routière du Centre',
    '2026-07-10', 'NORMAL', 'IN_PROGRESS',
    NULL, NULL, NULL, NULL, NULL, NULL,
    JSON_OBJECT('montant', 12500000, 'devise', 'XAF', 'numeroDecompte', 3),
    NOW(6), NOW(6)
);

INSERT INTO file_passages (
    id, file_id, chain_step_template_id, step_order, responsible_user_id,
    status, received_at, transmitted_at, due_at, created_at, updated_at
)
SELECT
    UNHEX(REPLACE(UUID(), '-', '')),
    @f3,
    s.id,
    s.step_order,
    UNHEX(REPLACE(JSON_UNQUOTE(JSON_EXTRACT(
        pd.step_assignments,
        CONCAT(
            '$."',
            LOWER(CONCAT(
                SUBSTR(HEX(s.id), 1, 8), '-',
                SUBSTR(HEX(s.id), 9, 4), '-',
                SUBSTR(HEX(s.id), 13, 4), '-',
                SUBSTR(HEX(s.id), 17, 4), '-',
                SUBSTR(HEX(s.id), 21, 12)
            )),
            '"'
        )
    )), '-', '')),
    CASE
        WHEN s.step_order = m.min_order THEN 'COMPLETED'
        WHEN s.step_order = m.min_order + 1 THEN 'IN_PROGRESS'
        ELSE 'PENDING'
    END,
    CASE
        WHEN s.step_order = m.min_order THEN '2026-07-10 09:00:00.000000'
        WHEN s.step_order = m.min_order + 1 THEN '2026-07-14 10:15:00.000000'
        ELSE NULL
    END,
    CASE
        WHEN s.step_order = m.min_order THEN '2026-07-14 10:15:00.000000'
        ELSE NULL
    END,
    CASE
        WHEN s.step_order = m.min_order THEN '2026-07-13 17:00:00.000000'
        WHEN s.step_order = m.min_order + 1 THEN DATE_ADD('2026-07-14 10:15:00', INTERVAL s.delay_value DAY)
        ELSE NULL
    END,
    NOW(6), NOW(6)
FROM preconfigured_dossiers pd
JOIN chain_step_templates s ON s.chain_template_id = pd.chain_template_id
JOIN (
    SELECT chain_template_id, MIN(step_order) AS min_order
    FROM chain_step_templates
    GROUP BY chain_template_id
) m ON m.chain_template_id = pd.chain_template_id
WHERE pd.code = 'DECOMPTE';

-- ── 4. AUTH-TRAV — en cours urgent ──
SET @pd := (SELECT id FROM preconfigured_dossiers WHERE code = 'AUTH-TRAV' LIMIT 1);
SET @ct := (SELECT chain_template_id FROM preconfigured_dossiers WHERE code = 'AUTH-TRAV' LIMIT 1);
SET @org := (SELECT id FROM organization WHERE code = 'DRTP-C' LIMIT 1);
SET @creator := COALESCE(
    (SELECT u.id FROM users u WHERE u.organization_id = @org AND u.active = 1 ORDER BY u.created_at LIMIT 1),
    @u_fallback
);

INSERT INTO files (
    id, reference_number, file_type_code, preconfigured_dossier_id, chain_template_id,
    organization_id, created_by_user_id, portal_user_id,
    subject, sender_or_beneficiary, received_at, priority, status,
    closure_reason, closed_at, cancellation_reason, cancelled_at,
    external_hold_reason, external_hold_since, metadata, created_at, updated_at
) VALUES (
    @f4, 'MINTP-DRTP-C-2026-0102', 'AUTH-TRAV', @pd, @ct,
    @org, @creator, NULL,
    'Autorisation travaux - pose fibre optique Avenue Kennedy',
    'CAMTEL',
    '2026-07-18', 'URGENT', 'IN_PROGRESS',
    NULL, NULL, NULL, NULL, NULL, NULL,
    JSON_OBJECT('localisation', 'Yaoundé — Avenue Kennedy', 'dureeJours', 10),
    NOW(6), NOW(6)
);

INSERT INTO file_passages (
    id, file_id, chain_step_template_id, step_order, responsible_user_id,
    status, received_at, transmitted_at, due_at, created_at, updated_at
)
SELECT
    UNHEX(REPLACE(UUID(), '-', '')),
    @f4,
    s.id,
    s.step_order,
    UNHEX(REPLACE(JSON_UNQUOTE(JSON_EXTRACT(
        pd.step_assignments,
        CONCAT(
            '$."',
            LOWER(CONCAT(
                SUBSTR(HEX(s.id), 1, 8), '-',
                SUBSTR(HEX(s.id), 9, 4), '-',
                SUBSTR(HEX(s.id), 13, 4), '-',
                SUBSTR(HEX(s.id), 17, 4), '-',
                SUBSTR(HEX(s.id), 21, 12)
            )),
            '"'
        )
    )), '-', '')),
    CASE WHEN s.step_order = m.min_order THEN 'IN_PROGRESS' ELSE 'PENDING' END,
    CASE WHEN s.step_order = m.min_order THEN '2026-07-18 11:00:00.000000' ELSE NULL END,
    NULL,
    CASE
        WHEN s.step_order = m.min_order
        THEN DATE_ADD('2026-07-18 11:00:00', INTERVAL s.delay_value DAY)
        ELSE NULL
    END,
    NOW(6), NOW(6)
FROM preconfigured_dossiers pd
JOIN chain_step_templates s ON s.chain_template_id = pd.chain_template_id
JOIN (
    SELECT chain_template_id, MIN(step_order) AS min_order
    FROM chain_step_templates
    GROUP BY chain_template_id
) m ON m.chain_template_id = pd.chain_template_id
WHERE pd.code = 'AUTH-TRAV';

-- ── 5. MISSION — clôturé ──
SET @pd := (SELECT id FROM preconfigured_dossiers WHERE code = 'MISSION' LIMIT 1);
SET @ct := (SELECT chain_template_id FROM preconfigured_dossiers WHERE code = 'MISSION' LIMIT 1);
SET @org := (SELECT id FROM organization WHERE code = 'DAG' LIMIT 1);
SET @creator := COALESCE(
    (SELECT u.id FROM users u WHERE u.organization_id = @org AND u.active = 1 ORDER BY u.created_at LIMIT 1),
    @u_fallback
);

INSERT INTO files (
    id, reference_number, file_type_code, preconfigured_dossier_id, chain_template_id,
    organization_id, created_by_user_id, portal_user_id,
    subject, sender_or_beneficiary, received_at, priority, status,
    closure_reason, closed_at, cancellation_reason, cancelled_at,
    external_hold_reason, external_hold_since, metadata, created_at, updated_at
) VALUES (
    @f5, 'MINTP-DAG-2026-0101', 'MISSION', @pd, @ct,
    @org, @creator, NULL,
    'Ordre de mission - supervision chantier RN4 Ebolowa',
    'Service Courrier DAG',
    '2026-06-15', 'NORMAL', 'CLOSED',
    'Mission exécutée - rapport de supervision archivé',
    '2026-07-05 16:00:00.000000',
    NULL, NULL, NULL, NULL,
    JSON_OBJECT('destination', 'Ebolowa', 'dureeJours', 5),
    NOW(6), NOW(6)
);

INSERT INTO file_passages (
    id, file_id, chain_step_template_id, step_order, responsible_user_id,
    status, received_at, transmitted_at, due_at, created_at, updated_at
)
SELECT
    UNHEX(REPLACE(UUID(), '-', '')),
    @f5,
    s.id,
    s.step_order,
    UNHEX(REPLACE(JSON_UNQUOTE(JSON_EXTRACT(
        pd.step_assignments,
        CONCAT(
            '$."',
            LOWER(CONCAT(
                SUBSTR(HEX(s.id), 1, 8), '-',
                SUBSTR(HEX(s.id), 9, 4), '-',
                SUBSTR(HEX(s.id), 13, 4), '-',
                SUBSTR(HEX(s.id), 17, 4), '-',
                SUBSTR(HEX(s.id), 21, 12)
            )),
            '"'
        )
    )), '-', '')),
    'COMPLETED',
    DATE_ADD('2026-06-15 09:00:00', INTERVAL (s.step_order - 1) DAY),
    DATE_ADD('2026-06-15 16:00:00', INTERVAL (s.step_order - 1) DAY),
    DATE_ADD('2026-06-16 17:00:00', INTERVAL (s.step_order - 1) DAY),
    NOW(6), NOW(6)
FROM preconfigured_dossiers pd
JOIN chain_step_templates s ON s.chain_template_id = pd.chain_template_id
WHERE pd.code = 'MISSION';

-- ── Séquences de numérotation ──
INSERT INTO file_number_sequences (organization_id, year, last_sequence)
SELECT id, 2026, 101 FROM organization WHERE code = 'DAG'
ON DUPLICATE KEY UPDATE last_sequence = GREATEST(last_sequence, 101);

INSERT INTO file_number_sequences (organization_id, year, last_sequence)
SELECT id, 2026, 101 FROM organization WHERE code = 'DIER'
ON DUPLICATE KEY UPDATE last_sequence = GREATEST(last_sequence, 101);

INSERT INTO file_number_sequences (organization_id, year, last_sequence)
SELECT id, 2026, 102 FROM organization WHERE code = 'DRTP-C'
ON DUPLICATE KEY UPDATE last_sequence = GREATEST(last_sequence, 102);

-- ── Vérification ──
SELECT
    f.reference_number,
    f.file_type_code,
    f.status,
    f.priority,
    pd.code AS preconfigured,
    o.code AS org,
    (SELECT COUNT(*) FROM file_passages p WHERE p.file_id = f.id) AS passages,
    (SELECT COUNT(*) FROM file_passages p WHERE p.file_id = f.id AND p.status = 'IN_PROGRESS') AS active_passages
FROM files f
JOIN organization o ON o.id = f.organization_id
LEFT JOIN preconfigured_dossiers pd ON pd.id = f.preconfigured_dossier_id
WHERE f.id IN (@f1, @f2, @f3, @f4, @f5)
ORDER BY f.file_type_code;
