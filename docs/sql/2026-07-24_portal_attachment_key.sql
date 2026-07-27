-- Objectif : clé de pièce jointe portail (formulaire préconfiguré)
-- Tables impactées : file_attachments
-- Prérequis : 2026-07-24_portal_foundations.sql
-- Exécution : manuelle sur MySQL (ddl-auto=none)

ALTER TABLE file_attachments
    ADD COLUMN portal_attachment_key VARCHAR(64) NULL AFTER uploaded_by_portal_user_id;

CREATE INDEX idx_attachments_portal_key ON file_attachments (file_id, portal_attachment_key);

-- rollback (commenté) :
-- DROP INDEX idx_attachments_portal_key ON file_attachments;
-- ALTER TABLE file_attachments DROP COLUMN portal_attachment_key;
