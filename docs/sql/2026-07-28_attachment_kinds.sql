-- Objectif : distinguer 3 types de pièces jointes (création / maillon / clôture)
-- Tables impactées : file_attachments
-- Prérequis : file_attachments, file_passages
-- Cible : PostgreSQL (Neon) — ddl-auto=none

ALTER TABLE file_attachments
    ADD COLUMN IF NOT EXISTS attachment_kind VARCHAR(20) NOT NULL DEFAULT 'CREATION';

ALTER TABLE file_attachments
    ADD COLUMN IF NOT EXISTS passage_id UUID NULL;

ALTER TABLE file_attachments
    ADD COLUMN IF NOT EXISTS portal_visible BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill depuis l'ancien booléen response_document
UPDATE file_attachments
SET attachment_kind = 'CLOSURE'
WHERE response_document IS TRUE
  AND (attachment_kind IS NULL OR attachment_kind = 'CREATION');

UPDATE file_attachments
SET attachment_kind = 'CREATION'
WHERE response_document IS NOT TRUE
  AND attachment_kind IS NULL;

-- FK maillon (nullable)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_attachment_passage'
    ) THEN
        ALTER TABLE file_attachments
            ADD CONSTRAINT fk_attachment_passage
            FOREIGN KEY (passage_id) REFERENCES file_passages(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_attachments_kind ON file_attachments (file_id, attachment_kind);
CREATE INDEX IF NOT EXISTS idx_attachments_passage ON file_attachments (passage_id);

-- rollback (commenté) :
-- ALTER TABLE file_attachments DROP CONSTRAINT IF EXISTS fk_attachment_passage;
-- DROP INDEX IF EXISTS idx_attachments_passage;
-- DROP INDEX IF EXISTS idx_attachments_kind;
-- ALTER TABLE file_attachments DROP COLUMN IF EXISTS portal_visible;
-- ALTER TABLE file_attachments DROP COLUMN IF EXISTS passage_id;
-- ALTER TABLE file_attachments DROP COLUMN IF EXISTS attachment_kind;
