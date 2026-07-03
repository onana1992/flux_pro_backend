-- Objectif : créer un utilisateur de démonstration pour chaque rôle système
-- Tables impactées : users, user_roles
-- Prérequis :
--   - organisation MINTP présente (DataInitializer ou seed orga)
--   - tables roles / user_roles présentes (docs/sql/2026-07-02_rbac_roles_permissions.sql)
--   - rôles seedés au démarrage de l'app (RbacDataInitializer)
-- Exécution : manuelle sur MySQL / MariaDB (ddl-auto=none)
-- Mot de passe commun : Fluxpro2026@
-- BCrypt cost 12 (Spring BCryptPasswordEncoder)
-- Idempotent : ré-exécutable (insert si absent, maj mot de passe, lien RBAC)

-- Hash BCrypt de « Fluxpro2026@ »
SET @pwd := '$2a$12$9VajABCCIisF4Swz2Uh1/.LkyJncWQ7wzo91XbMuI1NspPt2d8wOS';

-- ── 0. Organisations minimales (si absentes) ───────────────────────────────
INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c1000000-0000-4000-8000-000000000001', '-', '')), 'DAG',
       'Direction des Affaires Générales',
       '00000000-0000-4000-8000-000000000002', mintp.id, TRUE, NOW(6), NOW(6)
FROM organization mintp
WHERE mintp.code = 'MINTP'
  AND NOT EXISTS (SELECT 1 FROM organization o WHERE o.code = 'DAG');

INSERT INTO organization (id, code, name, organization_type_id, parent_id, active, created_at, updated_at)
SELECT UNHEX(REPLACE('c1000000-0000-4000-8000-000000000010', '-', '')), 'DSI',
       'Direction des Systèmes d''Information',
       '00000000-0000-4000-8000-000000000002', mintp.id, TRUE, NOW(6), NOW(6)
FROM organization mintp
WHERE mintp.code = 'MINTP'
  AND NOT EXISTS (SELECT 1 FROM organization o WHERE o.code = 'DSI');

-- ── 1. Utilisateurs (un par rôle) ───────────────────────────────────────────
-- UUID fixes b2000000-… pour idempotence

-- SUPER_ADMIN
INSERT INTO users (
    id, staff_number, email, last_name, first_name, phone, role,
    organization_id, job_title, password_hash,
    must_change_password, failed_login_attempts, locked_until, active,
    substitute_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE('b2000000-0000-4000-8000-000000000001', '-', '')),
    'SEED-SA-001', 'super.admin@mintp.cm', 'ADMIN', 'Super', '+237 600 00 00 01',
    'SUPER_ADMIN',
    COALESCE((SELECT id FROM organization WHERE code = 'DSI' LIMIT 1),
             (SELECT id FROM organization WHERE code = 'MINTP' LIMIT 1)),
    'Administrateur système (seed)', @pwd,
    FALSE, 0, NULL, TRUE, NULL, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'super.admin@mintp.cm');

-- BUSINESS_ADMIN
INSERT INTO users (
    id, staff_number, email, last_name, first_name, phone, role,
    organization_id, job_title, password_hash,
    must_change_password, failed_login_attempts, locked_until, active,
    substitute_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE('b2000000-0000-4000-8000-000000000002', '-', '')),
    'SEED-BA-001', 'business.admin@mintp.cm', 'ADMIN', 'Métier', '+237 600 00 00 02',
    'BUSINESS_ADMIN',
    COALESCE((SELECT id FROM organization WHERE code = 'DSI' LIMIT 1),
             (SELECT id FROM organization WHERE code = 'MINTP' LIMIT 1)),
    'Administrateur métier (seed)', @pwd,
    FALSE, 0, NULL, TRUE, NULL, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'business.admin@mintp.cm');

-- EXECUTIVE_OFFICE
INSERT INTO users (
    id, staff_number, email, last_name, first_name, phone, role,
    organization_id, job_title, password_hash,
    must_change_password, failed_login_attempts, locked_until, active,
    substitute_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE('b2000000-0000-4000-8000-000000000003', '-', '')),
    'SEED-EO-001', 'executive.office@mintp.cm', 'CABINET', 'Ministre', '+237 600 00 00 03',
    'EXECUTIVE_OFFICE',
    (SELECT id FROM organization WHERE code = 'MINTP' LIMIT 1),
    'Cabinet du Ministre (seed)', @pwd,
    FALSE, 0, NULL, TRUE, NULL, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'executive.office@mintp.cm');

-- SECRETARY_GENERAL
INSERT INTO users (
    id, staff_number, email, last_name, first_name, phone, role,
    organization_id, job_title, password_hash,
    must_change_password, failed_login_attempts, locked_until, active,
    substitute_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE('b2000000-0000-4000-8000-000000000004', '-', '')),
    'SEED-SG-001', 'secretary.general@mintp.cm', 'GENERAL', 'Secrétaire', '+237 600 00 00 04',
    'SECRETARY_GENERAL',
    (SELECT id FROM organization WHERE code = 'MINTP' LIMIT 1),
    'Secrétaire général (seed)', @pwd,
    FALSE, 0, NULL, TRUE, NULL, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'secretary.general@mintp.cm');

-- DIRECTOR
INSERT INTO users (
    id, staff_number, email, last_name, first_name, phone, role,
    organization_id, job_title, password_hash,
    must_change_password, failed_login_attempts, locked_until, active,
    substitute_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE('b2000000-0000-4000-8000-000000000005', '-', '')),
    'SEED-DIR-001', 'director@mintp.cm', 'DIRECTEUR', 'Alice', '+237 600 00 00 05',
    'DIRECTOR',
    COALESCE((SELECT id FROM organization WHERE code = 'DAG' LIMIT 1),
             (SELECT id FROM organization WHERE code = 'MINTP' LIMIT 1)),
    'Directeur (seed)', @pwd,
    FALSE, 0, NULL, TRUE, NULL, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'director@mintp.cm');

-- SERVICE_HEAD
INSERT INTO users (
    id, staff_number, email, last_name, first_name, phone, role,
    organization_id, job_title, password_hash,
    must_change_password, failed_login_attempts, locked_until, active,
    substitute_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE('b2000000-0000-4000-8000-000000000006', '-', '')),
    'SEED-SH-001', 'service.head@mintp.cm', 'CHEF', 'Service', '+237 600 00 00 06',
    'SERVICE_HEAD',
    COALESCE((SELECT id FROM organization WHERE code = 'DAG' LIMIT 1),
             (SELECT id FROM organization WHERE code = 'MINTP' LIMIT 1)),
    'Chef de service (seed)', @pwd,
    FALSE, 0, NULL, TRUE, NULL, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'service.head@mintp.cm');

-- AGENT
INSERT INTO users (
    id, staff_number, email, last_name, first_name, phone, role,
    organization_id, job_title, password_hash,
    must_change_password, failed_login_attempts, locked_until, active,
    substitute_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE('b2000000-0000-4000-8000-000000000007', '-', '')),
    'SEED-AG-001', 'agent@mintp.cm', 'AGENT', 'Paul', '+237 600 00 00 07',
    'AGENT',
    COALESCE((SELECT id FROM organization WHERE code = 'DAG' LIMIT 1),
             (SELECT id FROM organization WHERE code = 'MINTP' LIMIT 1)),
    'Agent traitant (seed)', @pwd,
    FALSE, 0, NULL, TRUE, NULL, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'agent@mintp.cm');

-- SUPPORT
INSERT INTO users (
    id, staff_number, email, last_name, first_name, phone, role,
    organization_id, job_title, password_hash,
    must_change_password, failed_login_attempts, locked_until, active,
    substitute_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE('b2000000-0000-4000-8000-000000000008', '-', '')),
    'SEED-SUP-001', 'support@mintp.cm', 'SUPPORT', 'Marie', '+237 600 00 00 08',
    'SUPPORT',
    COALESCE((SELECT id FROM organization WHERE code = 'DAG' LIMIT 1),
             (SELECT id FROM organization WHERE code = 'MINTP' LIMIT 1)),
    'Support / courrier (seed)', @pwd,
    FALSE, 0, NULL, TRUE, NULL, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'support@mintp.cm');

-- READER
INSERT INTO users (
    id, staff_number, email, last_name, first_name, phone, role,
    organization_id, job_title, password_hash,
    must_change_password, failed_login_attempts, locked_until, active,
    substitute_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE('b2000000-0000-4000-8000-000000000009', '-', '')),
    'SEED-RD-001', 'reader@mintp.cm', 'LECTEUR', 'Jean', '+237 600 00 00 09',
    'READER',
    COALESCE((SELECT id FROM organization WHERE code = 'DAG' LIMIT 1),
             (SELECT id FROM organization WHERE code = 'MINTP' LIMIT 1)),
    'Lecteur (seed)', @pwd,
    FALSE, 0, NULL, TRUE, NULL, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'reader@mintp.cm');

-- REGIONAL_DIRECTOR
INSERT INTO users (
    id, staff_number, email, last_name, first_name, phone, role,
    organization_id, job_title, password_hash,
    must_change_password, failed_login_attempts, locked_until, active,
    substitute_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE('b2000000-0000-4000-8000-00000000000a', '-', '')),
    'SEED-RDIR-001', 'regional.director@mintp.cm', 'DRTP', 'Centre', '+237 600 00 00 10',
    'REGIONAL_DIRECTOR',
    COALESCE((SELECT id FROM organization WHERE code = 'DRTP-C' LIMIT 1),
             (SELECT id FROM organization WHERE code = 'MINTP' LIMIT 1)),
    'Directeur régional (seed)', @pwd,
    FALSE, 0, NULL, TRUE, NULL, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'regional.director@mintp.cm');

-- ── 2. Réinitialiser le mot de passe des comptes seed (ré-exécution) ───────
UPDATE users
SET password_hash = @pwd,
    must_change_password = FALSE,
    failed_login_attempts = 0,
    locked_until = NULL,
    active = TRUE,
    updated_at = NOW(6)
WHERE email IN (
    'super.admin@mintp.cm',
    'business.admin@mintp.cm',
    'executive.office@mintp.cm',
    'secretary.general@mintp.cm',
    'director@mintp.cm',
    'service.head@mintp.cm',
    'agent@mintp.cm',
    'support@mintp.cm',
    'reader@mintp.cm',
    'regional.director@mintp.cm'
);

-- ── 3. Lier chaque utilisateur au rôle RBAC correspondant (roles.name) ─────
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
INNER JOIN roles r ON r.name = u.role
WHERE u.email IN (
    'super.admin@mintp.cm',
    'business.admin@mintp.cm',
    'executive.office@mintp.cm',
    'secretary.general@mintp.cm',
    'director@mintp.cm',
    'service.head@mintp.cm',
    'agent@mintp.cm',
    'support@mintp.cm',
    'reader@mintp.cm',
    'regional.director@mintp.cm'
)
AND NOT EXISTS (
    SELECT 1 FROM user_roles ur
    WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- rollback (commenté) :
-- DELETE FROM user_roles
-- WHERE user_id IN (
--     SELECT id FROM users WHERE email IN (
--         'super.admin@mintp.cm', 'business.admin@mintp.cm', 'executive.office@mintp.cm',
--         'secretary.general@mintp.cm', 'director@mintp.cm', 'service.head@mintp.cm',
--         'agent@mintp.cm', 'support@mintp.cm', 'reader@mintp.cm', 'regional.director@mintp.cm'
--     )
-- );
-- DELETE FROM users WHERE email IN (
--     'super.admin@mintp.cm', 'business.admin@mintp.cm', 'executive.office@mintp.cm',
--     'secretary.general@mintp.cm', 'director@mintp.cm', 'service.head@mintp.cm',
--     'agent@mintp.cm', 'support@mintp.cm', 'reader@mintp.cm', 'regional.director@mintp.cm'
-- );
