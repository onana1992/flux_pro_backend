-- Objectif : seed comptes portail internes + externes
-- Mot de passe (internes) : Fluxpro2026@
-- Externes : OTP à la connexion ; hash stocké pour cohérence seed ; email vérifié
-- Prérequis : portal_users, organization, users (e.fotso)
-- Idempotent sur email
-- Exécution : mysql --default-character-set=utf8mb4 …

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @pwd := '$2a$12$9VajABCCIisF4Swz2Uh1/.LkyJncWQ7wzo91XbMuI1NspPt2d8wOS';
SET @admin_id := (SELECT id FROM users WHERE email = 'e.fotso@mintp.cm' LIMIT 1);

-- ═══════════════════════════════════════════════════════════════════════════
-- INTERNES (INTERNAL_EMPLOYEE) — login email + mot de passe
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO portal_users (
    id, email, first_name, last_name, phone, portal_user_type, staff_number,
    organization_id, password_hash, must_change_password, password_changed_at,
    active, email_verified_at, created_by_admin_user_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000001', '-', '')),
    'paul.ngono@mintp.cm', 'Paul', 'NGONO', '+237 670 11 00 01',
    'INTERNAL_EMPLOYEE', 'MAT-2016-0101',
    (SELECT id FROM organization WHERE code = 'DAG-RH' LIMIT 1),
    @pwd, FALSE, NOW(6), TRUE, NOW(6), @admin_id, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM portal_users WHERE email = 'paul.ngono@mintp.cm');

INSERT INTO portal_users (
    id, email, first_name, last_name, phone, portal_user_type, staff_number,
    organization_id, password_hash, must_change_password, password_changed_at,
    active, email_verified_at, created_by_admin_user_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000002', '-', '')),
    'marie.essomba@mintp.cm', 'Marie', 'ESSOMBA', '+237 670 11 00 02',
    'INTERNAL_EMPLOYEE', 'MAT-2018-0102',
    (SELECT id FROM organization WHERE code = 'DAG' LIMIT 1),
    @pwd, FALSE, NOW(6), TRUE, NOW(6), @admin_id, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM portal_users WHERE email = 'marie.essomba@mintp.cm');

INSERT INTO portal_users (
    id, email, first_name, last_name, phone, portal_user_type, staff_number,
    organization_id, password_hash, must_change_password, password_changed_at,
    active, email_verified_at, created_by_admin_user_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000003', '-', '')),
    'jean.atangana@portal.mintp.cm', 'Jean', 'ATANGANA', '+237 670 11 00 03',
    'INTERNAL_EMPLOYEE', 'MAT-2015-0103',
    (SELECT id FROM organization WHERE code = 'DRTP-C' LIMIT 1),
    @pwd, FALSE, NOW(6), TRUE, NOW(6), @admin_id, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM portal_users WHERE email = 'jean.atangana@portal.mintp.cm');

INSERT INTO portal_users (
    id, email, first_name, last_name, phone, portal_user_type, staff_number,
    organization_id, password_hash, must_change_password, password_changed_at,
    active, email_verified_at, created_by_admin_user_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000004', '-', '')),
    'claire.fouda@mintp.cm', 'Claire', 'FOUDA', '+237 670 11 00 04',
    'INTERNAL_EMPLOYEE', 'MAT-2019-0104',
    (SELECT id FROM organization WHERE code = 'DIER' LIMIT 1),
    @pwd, FALSE, NOW(6), TRUE, NOW(6), @admin_id, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM portal_users WHERE email = 'claire.fouda@mintp.cm');

INSERT INTO portal_users (
    id, email, first_name, last_name, phone, portal_user_type, staff_number,
    organization_id, password_hash, must_change_password, password_changed_at,
    active, email_verified_at, created_by_admin_user_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000005', '-', '')),
    'serge.mballa@mintp.cm', 'Serge', 'MBALLA', '+237 670 11 00 05',
    'INTERNAL_EMPLOYEE', 'MAT-2017-0105',
    (SELECT id FROM organization WHERE code = 'DGTI' LIMIT 1),
    @pwd, FALSE, NOW(6), TRUE, NOW(6), @admin_id, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM portal_users WHERE email = 'serge.mballa@mintp.cm');

-- ═══════════════════════════════════════════════════════════════════════════
-- EXTERNES (EXTERNAL) — connexion OTP ; email déjà vérifié pour tests
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO portal_users (
    id, email, first_name, last_name, phone, portal_user_type, staff_number,
    organization_id, password_hash, must_change_password, password_changed_at,
    active, email_verified_at, created_by_admin_user_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000011', '-', '')),
    'contact@btp-cameroun.cm', 'Alain', 'OWONA', '+237 699 20 00 11',
    'EXTERNAL', NULL, NULL,
    @pwd, FALSE, NOW(6), TRUE, NOW(6), @admin_id, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM portal_users WHERE email = 'contact@btp-cameroun.cm');

INSERT INTO portal_users (
    id, email, first_name, last_name, phone, portal_user_type, staff_number,
    organization_id, password_hash, must_change_password, password_changed_at,
    active, email_verified_at, created_by_admin_user_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000012', '-', '')),
    'fatou.ndjock@gmail.com', 'Fatou', 'NDJOCK', '+237 699 20 00 12',
    'EXTERNAL', NULL, NULL,
    @pwd, FALSE, NOW(6), TRUE, NOW(6), @admin_id, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM portal_users WHERE email = 'fatou.ndjock@gmail.com');

INSERT INTO portal_users (
    id, email, first_name, last_name, phone, portal_user_type, staff_number,
    organization_id, password_hash, must_change_password, password_changed_at,
    active, email_verified_at, created_by_admin_user_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000013', '-', '')),
    'directeur@routes-sa.cm', 'Bernard', 'KAMGA', '+237 699 20 00 13',
    'EXTERNAL', NULL, NULL,
    @pwd, FALSE, NOW(6), TRUE, NOW(6), @admin_id, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM portal_users WHERE email = 'directeur@routes-sa.cm');

INSERT INTO portal_users (
    id, email, first_name, last_name, phone, portal_user_type, staff_number,
    organization_id, password_hash, must_change_password, password_changed_at,
    active, email_verified_at, created_by_admin_user_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE('a1000000-0000-4000-8000-000000000014', '-', '')),
    'info@genie-civil.cm', 'Hélène', 'TALLA', '+237 699 20 00 14',
    'EXTERNAL', NULL, NULL,
    @pwd, FALSE, NOW(6), TRUE, NOW(6), @admin_id, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM portal_users WHERE email = 'info@genie-civil.cm');

-- Aligner mot de passe / flags sur TOUS les comptes portail
UPDATE portal_users
SET
    password_hash = @pwd,
    must_change_password = FALSE,
    password_changed_at = COALESCE(password_changed_at, NOW(6)),
    email_verified_at = COALESCE(email_verified_at, NOW(6)),
    active = TRUE,
    updated_at = NOW(6);

SELECT portal_user_type, COUNT(*) AS n
FROM portal_users
GROUP BY portal_user_type;

SELECT email, first_name, last_name, portal_user_type, staff_number, active,
       must_change_password, email_verified_at IS NOT NULL AS verified
FROM portal_users
ORDER BY portal_user_type, email;
