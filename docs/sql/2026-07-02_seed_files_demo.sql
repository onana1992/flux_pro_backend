-- Objectif : jeux de démonstration — dossiers DOS (courriers, marchés, DRTP)
-- Tables impactées : organization (optionnel), files, file_number_sequences
-- Prérequis :
--   - docs/sql/2026-07-02_files.sql exécuté
--   - file_types + chain_templates seedés (démarrage app ou scripts CHN-TPL)
--   - MINTP présent ; utilisateur e.fotso@mintp.cm (ou tout user actif)
-- Exécution : manuelle sur MySQL / MariaDB (ddl-auto=none)
-- UUID : UNHEX(REPLACE('…', '-', '')) — compatible MariaDB 10.4 (pas de UUID_TO_BIN)
-- Idempotent : supprime puis ré-insère les dossiers seed (UUID fixes ci-dessous)

-- ── 0. Directions pilotes minimales (si import CSV organisations non fait) ──
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

-- DRTP-C : souvent déjà créée par DataInitializer ; rien à faire si présente

-- ── 1. Purge seed précédent ──
DELETE FROM file_attachments
WHERE file_id IN (
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000001', '-', '')),
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000002', '-', '')),
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000003', '-', '')),
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000004', '-', '')),
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000005', '-', '')),
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000006', '-', '')),
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000007', '-', '')),
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000008', '-', ''))
);

DELETE FROM files
WHERE id IN (
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000001', '-', '')),
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000002', '-', '')),
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000003', '-', '')),
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000004', '-', '')),
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000005', '-', '')),
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000006', '-', '')),
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000007', '-', '')),
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000008', '-', ''))
);

-- ── 2. Dossiers de démonstration ──
-- created_by : agent DAG si import CSV agents, sinon SUPER_ADMIN démo

INSERT INTO files (
    id, reference_number, file_type_code, chain_template_id,
    organization_id, created_by_user_id,
    subject, sender_or_beneficiary, received_at,
    priority, status,
    closure_reason, closed_at,
    cancellation_reason, cancelled_at,
    metadata, created_at, updated_at
)
VALUES
-- DOS-SEED-01 : brouillon courrier DAG (modifiable, sans numéro)
(
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000001', '-', '')),
    NULL,
    'COUR-STD',
    NULL,
    (SELECT id FROM organization WHERE code = 'DAG' LIMIT 1),
    COALESCE(
        (SELECT id FROM users WHERE email = 'r.tchakoute@mintp.cm' LIMIT 1),
        (SELECT id FROM users WHERE email = 'e.fotso@mintp.cm' LIMIT 1)
    ),
    'Demande de renseignements — permis de construire route communale',
    'Mairie d''Obala',
    '2026-06-10',
    'NORMAL',
    'DRAFT',
    NULL, NULL, NULL, NULL, NULL,
    NOW(6), NOW(6)
),
-- DOS-SEED-02 : courrier soumis — T01
(
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000002', '-', '')),
    'MINTP-DAG-2026-0001',
    'COUR-STD',
    (SELECT id FROM chain_templates WHERE code = 'T01' LIMIT 1),
    (SELECT id FROM organization WHERE code = 'DAG' LIMIT 1),
    COALESCE(
        (SELECT id FROM users WHERE email = 'c.abanda@mintp.cm' LIMIT 1),
        (SELECT id FROM users WHERE email = 'e.fotso@mintp.cm' LIMIT 1)
    ),
    'Invitation cérémonie pose de première pierre — pont Nyong',
    'Ministère des Finances',
    '2026-06-12',
    'NORMAL',
    'IN_PROGRESS',
    NULL, NULL, NULL, NULL, NULL,
    NOW(6), NOW(6)
),
-- DOS-SEED-03 : très urgent — template T02 (DOS-06c)
(
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000003', '-', '')),
    'MINTP-DAG-2026-0002',
    'COUR-STD',
    (SELECT id FROM chain_templates WHERE code = 'T02' LIMIT 1),
    (SELECT id FROM organization WHERE code = 'DAG' LIMIT 1),
    COALESCE(
        (SELECT id FROM users WHERE email = 'f.onaha@mintp.cm' LIMIT 1),
        (SELECT id FROM users WHERE email = 'e.fotso@mintp.cm' LIMIT 1)
    ),
    'Courrier recommandé — société privée BTP (réponse sous 48 h)',
    'Société Cameroun BTP SARL',
    '2026-06-18',
    'VERY_URGENT',
    'IN_PROGRESS',
    NULL, NULL, NULL, NULL, NULL,
    NOW(6), NOW(6)
),
-- DOS-SEED-04 : marché simplifié DIER — T03
(
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000004', '-', '')),
    'MINTP-DIER-2026-0001',
    'MARCHE-SMP',
    (SELECT id FROM chain_templates WHERE code = 'T03' LIMIT 1),
    (SELECT id FROM organization WHERE code = 'DIER' LIMIT 1),
    COALESCE(
        (SELECT id FROM users WHERE email = 'l.bapoo@mintp.cm' LIMIT 1),
        (SELECT id FROM users WHERE email = 'e.fotso@mintp.cm' LIMIT 1)
    ),
    'Marché simplifié — entretien route nationale N°3 tronçon Mbalmayo',
    'Entreprise Routière du Centre',
    '2026-06-05',
    'NORMAL',
    'IN_PROGRESS',
    NULL, NULL, NULL, NULL,
    JSON_OBJECT('montantEstime', 45000000, 'devise', 'XAF'),
    NOW(6), NOW(6)
),
-- DOS-SEED-05 : autorisation travaux DRTP Centre — T04
(
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000005', '-', '')),
    'MINTP-DRTP-C-2026-0001',
    'AUTH-TRAV',
    (SELECT id FROM chain_templates WHERE code = 'T04' LIMIT 1),
    (SELECT id FROM organization WHERE code = 'DRTP-C' LIMIT 1),
    COALESCE(
        (SELECT id FROM users WHERE email = 's.adjomo@mintp.cm' LIMIT 1),
        (SELECT id FROM users WHERE email = 'e.fotso@mintp.cm' LIMIT 1)
    ),
    'Autorisation travaux — ouverture tranchée fibre optique Boulevard du 20 Mai',
    'CAMTEL',
    '2026-06-20',
    'URGENT',
    'IN_PROGRESS',
    NULL, NULL, NULL, NULL,
    JSON_OBJECT('localisation', 'Yaoundé — Bastos', 'dureeJours', 14),
    NOW(6), NOW(6)
),
-- DOS-SEED-06 : brouillon DRTP (test périmètre régional)
(
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000006', '-', '')),
    NULL,
    'AUTH-TRAV',
    NULL,
    (SELECT id FROM organization WHERE code = 'DRTP-C' LIMIT 1),
    COALESCE(
        (SELECT id FROM users WHERE email = 'e.fotso@mintp.cm' LIMIT 1),
        (SELECT id FROM users LIMIT 1)
    ),
    'Demande autorisation — pose signalisation provisoire chantier',
    'Commune d''Yaoundé V',
    '2026-06-25',
    'NORMAL',
    'DRAFT',
    NULL, NULL, NULL, NULL, NULL,
    NOW(6), NOW(6)
),
-- DOS-SEED-07 : clôturé (DOS-11)
(
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000007', '-', '')),
    'MINTP-DAG-2026-0003',
    'COUR-STD',
    (SELECT id FROM chain_templates WHERE code = 'T01' LIMIT 1),
    (SELECT id FROM organization WHERE code = 'DAG' LIMIT 1),
    COALESCE(
        (SELECT id FROM users WHERE email = 'd.atangana@mintp.cm' LIMIT 1),
        (SELECT id FROM users WHERE email = 'e.fotso@mintp.cm' LIMIT 1)
    ),
    'Transmission compte rendu réunion interministérielle infrastructures',
    'Primature',
    '2026-05-28',
    'NORMAL',
    'CLOSED',
    'Réponse signée et expédiée au cabinet — accusé de réception reçu',
    '2026-06-15 16:30:00.000000',
    NULL, NULL, NULL,
    NOW(6), NOW(6)
),
-- DOS-SEED-08 : annulé (motif tracé)
(
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000008', '-', '')),
    NULL,
    'COUR-STD',
    NULL,
    (SELECT id FROM organization WHERE code = 'DAG' LIMIT 1),
    COALESCE(
        (SELECT id FROM users WHERE email = 'j.mengue@mintp.cm' LIMIT 1),
        (SELECT id FROM users WHERE email = 'e.fotso@mintp.cm' LIMIT 1)
    ),
    'Doublon — même courrier déjà enregistré sous MINTP-DAG-2026-0001',
    'Cabinet partenaire',
    '2026-06-14',
    'NORMAL',
    'CANCELLED',
    NULL, NULL,
    'Dossier en double détecté à la réception — fusion avec MINTP-DAG-2026-0001',
    '2026-06-14 11:00:00.000000',
    NULL,
    NOW(6), NOW(6)
);

-- ── 3. Séquences de numérotation 2026 (alignées sur les numéros ci-dessus) ──
INSERT INTO file_number_sequences (organization_id, year, last_sequence)
SELECT o.id, 2026, 3
FROM organization o
WHERE o.code = 'DAG'
ON DUPLICATE KEY UPDATE last_sequence = GREATEST(last_sequence, 3);

INSERT INTO file_number_sequences (organization_id, year, last_sequence)
SELECT o.id, 2026, 1
FROM organization o
WHERE o.code = 'DIER'
ON DUPLICATE KEY UPDATE last_sequence = GREATEST(last_sequence, 1);

INSERT INTO file_number_sequences (organization_id, year, last_sequence)
SELECT o.id, 2026, 1
FROM organization o
WHERE o.code = 'DRTP-C'
ON DUPLICATE KEY UPDATE last_sequence = GREATEST(last_sequence, 1);

-- ── Vérification rapide ──
-- SELECT BIN_TO_UUID(id) AS id, reference_number, file_type_code, status, priority
-- FROM files
-- WHERE id IN (
--     UNHEX(REPLACE('a1000000-0000-4000-8000-000000000001', '-', '')),
--     UNHEX(REPLACE('a1000000-0000-4000-8000-000000000002', '-', ''))
-- );

-- rollback (commenté) :
-- DELETE FROM files WHERE id LIKE UNHEX(REPLACE('a1000000-0000-4000-8000-%', '-', '')); -- non supporté
-- Ré-exécuter la section 1 (DELETE) ci-dessus
