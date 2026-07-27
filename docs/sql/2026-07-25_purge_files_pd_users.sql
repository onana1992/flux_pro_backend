-- Objectif : purger dossiers, dossiers préconfigurés et utilisateurs internes
-- Conservé : organisations, file_types, chain_templates/steps, roles/permissions, portal_users (lien admin nullifié)
-- Exécution : manuelle MySQL/MariaDB

SET FOREIGN_KEY_CHECKS = 0;

-- ── 1. Dossiers et dépendances ───────────────────────────────────────────────
DELETE FROM file_passage_cc;
DELETE FROM file_passages;
DELETE FROM file_attachments;
DELETE FROM alerts;
DELETE FROM files;
DELETE FROM file_number_sequences;

-- ── 2. Dossiers préconfigurés ────────────────────────────────────────────────
DELETE FROM preconfigured_dossiers;

-- ── 3. Utilisateurs (FK d'abord) ──────────────────────────────────────────────
UPDATE portal_users SET created_by_admin_user_id = NULL;
UPDATE admin_audit_log SET actor_user_id = NULL WHERE actor_user_id IS NOT NULL;
UPDATE users SET substitute_id = NULL;

DELETE FROM refresh_token;
DELETE FROM login_audit;
DELETE FROM user_roles;
DELETE FROM users;

SET FOREIGN_KEY_CHECKS = 1;

SELECT
    (SELECT COUNT(*) FROM files) AS files_left,
    (SELECT COUNT(*) FROM preconfigured_dossiers) AS pd_left,
    (SELECT COUNT(*) FROM users) AS users_left;
