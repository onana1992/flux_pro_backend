-- Objectif : réaligner l'attribution des pièces jointes de maillon sur le responsable du maillon
-- Tables impactées : file_attachments
-- Prérequis : file_attachments.passage_id, file_passages.responsible_user_id
-- Cible : PostgreSQL (Neon)
-- Date : 2026-08-08

-- Cas : pièce PASSAGE déjà liée à un maillon mais uploaded_by_id = autre utilisateur
-- (ex. créateur / admin qui a uploadé à la place du titulaire).

BEGIN;

UPDATE file_attachments a
SET uploaded_by_id = p.responsible_user_id
FROM file_passages p
WHERE a.passage_id = p.id
  AND a.attachment_kind = 'PASSAGE'
  AND p.responsible_user_id IS NOT NULL
  AND (a.uploaded_by_id IS DISTINCT FROM p.responsible_user_id);

SELECT
    COUNT(*) FILTER (
        WHERE a.attachment_kind = 'PASSAGE'
          AND a.passage_id IS NOT NULL
          AND a.uploaded_by_id = p.responsible_user_id
    ) AS passage_aligned,
    COUNT(*) FILTER (
        WHERE a.attachment_kind = 'PASSAGE' AND a.passage_id IS NOT NULL
    ) AS passage_total
FROM file_attachments a
LEFT JOIN file_passages p ON p.id = a.passage_id;

COMMIT;

-- rollback : restaurer depuis backup (UPDATE irréversible sans journal des anciennes valeurs)
