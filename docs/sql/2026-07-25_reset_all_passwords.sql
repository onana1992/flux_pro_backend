-- Objectif : mot de passe commun Fluxpro2026@ + must_change_password = false
-- Tables : users, portal_users
-- Hash BCrypt cost 12 (Spring BCryptPasswordEncoder)
-- Exécution : mysql --default-character-set=utf8mb4 …

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @pwd := '$2a$12$9VajABCCIisF4Swz2Uh1/.LkyJncWQ7wzo91XbMuI1NspPt2d8wOS';

UPDATE users
SET
    password_hash = @pwd,
    must_change_password = FALSE,
    failed_login_attempts = 0,
    locked_until = NULL,
    updated_at = NOW(6);

UPDATE portal_users
SET
    password_hash = @pwd,
    must_change_password = FALSE,
    password_changed_at = NOW(6),
    updated_at = NOW(6)
WHERE password_hash IS NOT NULL;

SELECT
    (SELECT COUNT(*) FROM users WHERE must_change_password = FALSE) AS users_ok,
    (SELECT COUNT(*) FROM portal_users WHERE must_change_password = FALSE) AS portal_ok;
