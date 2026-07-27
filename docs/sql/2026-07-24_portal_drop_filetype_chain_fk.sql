-- Objectif : retirer les FK FileType → chaîne / responsable portail
-- Tables impactées : file_types
-- Prérequis : docs/sql/2026-07-24_portal_foundations.sql déjà appliqué
-- Exécution : manuelle sur MySQL / MariaDB (ddl-auto=none)
--
-- La chaîne se résout via chain_templates.file_type_code = file_types.code
-- Le responsable 1ᵉʳ maillon se résout via responsible_role du stage 1 + org du dossier

ALTER TABLE file_types DROP FOREIGN KEY fk_file_types_portal_chain;
ALTER TABLE file_types DROP FOREIGN KEY fk_file_types_portal_responsible;

ALTER TABLE file_types
    DROP COLUMN portal_default_first_step_responsible_user_id,
    DROP COLUMN portal_chain_template_id;

-- rollback (commenté) :
-- ALTER TABLE file_types
--     ADD COLUMN portal_chain_template_id BINARY(16) NULL AFTER portal_audience,
--     ADD COLUMN portal_default_first_step_responsible_user_id BINARY(16) NULL AFTER portal_chain_template_id;
-- ALTER TABLE file_types
--     ADD CONSTRAINT fk_file_types_portal_chain
--         FOREIGN KEY (portal_chain_template_id) REFERENCES chain_templates(id),
--     ADD CONSTRAINT fk_file_types_portal_responsible
--         FOREIGN KEY (portal_default_first_step_responsible_user_id) REFERENCES users(id);
