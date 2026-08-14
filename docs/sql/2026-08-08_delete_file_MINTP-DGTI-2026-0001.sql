-- Objectif : supprimer définitivement le dossier MINTP-DGTI-2026-0001 et ses dépendances
-- Tables impactées : file_passage_cc, alerts, file_passages, file_attachments, files
-- Prérequis : PostgreSQL (Neon) — référence exacte ci-dessous
-- Exécution : manuelle (psql) — ddl-auto=none
-- Date : 2026-08-08

BEGIN;

-- Vérification préalable
SELECT id, reference_number, status, subject
FROM files
WHERE reference_number = 'MINTP-DGTI-2026-0001';

-- CC des maillons
DELETE FROM file_passage_cc
WHERE file_passage_id IN (
    SELECT p.id
    FROM file_passages p
    JOIN files f ON f.id = p.file_id
    WHERE f.reference_number = 'MINTP-DGTI-2026-0001'
);

-- Alertes liées au dossier ou à ses passages
DELETE FROM alerts
WHERE file_id IN (
    SELECT id FROM files WHERE reference_number = 'MINTP-DGTI-2026-0001'
)
OR file_passage_id IN (
    SELECT p.id
    FROM file_passages p
    JOIN files f ON f.id = p.file_id
    WHERE f.reference_number = 'MINTP-DGTI-2026-0001'
);

-- Passages
DELETE FROM file_passages
WHERE file_id IN (
    SELECT id FROM files WHERE reference_number = 'MINTP-DGTI-2026-0001'
);

-- Pièces jointes (métadonnées uniquement ; objets storage hors scope)
DELETE FROM file_attachments
WHERE file_id IN (
    SELECT id FROM files WHERE reference_number = 'MINTP-DGTI-2026-0001'
);

-- Dossier
DELETE FROM files
WHERE reference_number = 'MINTP-DGTI-2026-0001';

-- Contrôle
SELECT COUNT(*) AS remaining
FROM files
WHERE reference_number = 'MINTP-DGTI-2026-0001';

COMMIT;

-- rollback :
-- BEGIN; … restaurer depuis backup uniquement (DELETE irréversible) …
