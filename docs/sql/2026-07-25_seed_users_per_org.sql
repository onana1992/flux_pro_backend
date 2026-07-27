-- Objectif : 1 utilisateur actif par organisation + couverture de tous les rôles
-- Noms/prénoms : typiques du Cameroun (UTF-8)
-- Mot de passe commun : Fluxpro2026@
-- Hash BCrypt cost 12
-- Prérequis : organizations actives, roles synchronisés, users vidés
-- Idempotent sur email
-- Exécution : mysql --default-character-set=utf8mb4 …

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @pwd := '$2a$12$9VajABCCIisF4Swz2Uh1/.LkyJncWQ7wzo91XbMuI1NspPt2d8wOS';

CREATE TEMPORARY TABLE seed_cam_names (
    seq INT PRIMARY KEY,
    last_name VARCHAR(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    first_name VARCHAR(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=Memory;

INSERT INTO seed_cam_names (seq, last_name, first_name) VALUES
(1,  'ABEGA', 'Paul'),
(2,  'AMOUGOU', 'Christelle'),
(3,  'ATANGANA', 'Jean'),
(4,  'ATEBA', 'Sylvie'),
(5,  'BELINGA', 'Marcel'),
(6,  'BIKOI', 'Grace'),
(7,  'DJOUMESSI', 'Alain'),
(8,  'EBOGO', 'Patricia'),
(9,  'ESSOMBA', 'Joseph'),
(10, 'ETOA', 'Carine'),
(11, 'EVANE', 'Bertrand'),
(12, 'EVINA', 'Diane'),
(13, 'FEUDJIO', 'Hervé'),
(14, 'FOKOU', 'Lucie'),
(15, 'FOUDA', 'Georges'),
(16, 'KAMGA', 'Marie'),
(17, 'KUETE', 'Eric'),
(18, 'MANGA', 'Florence'),
(19, 'MBALLA', 'Pierre'),
(20, 'MEKA', 'Nicole'),
(21, 'MEKONGO', 'Samuel'),
(22, 'MVONDO', 'Jeanne'),
(23, 'NANA', 'Olivier'),
(24, 'NDJANA', 'Thérèse'),
(25, 'NDJOCK', 'Blaise'),
(26, 'NDONGO', 'Ingrid'),
(27, 'NGOA', 'William'),
(28, 'NGONO', 'Rachel'),
(29, 'NGUEMA', 'François'),
(30, 'NOMO', 'Valérie'),
(31, 'NTEME', 'Yannick'),
(32, 'OMGBA', 'Irène'),
(33, 'ONANA', 'Jacques'),
(34, 'OWONA', 'Gisèle'),
(35, 'TALLA', 'Donald'),
(36, 'TCHINDA', 'Élise'),
(37, 'TCHOUNKEU', 'Henri'),
(38, 'TSOGO', 'Laura'),
(39, 'ZANG', 'Ulrich'),
(40, 'BIYA', 'Aïcha'),
(41, 'FOTSO', 'Kevin'),
(42, 'MBARGA', 'Fatou'),
(43, 'NGALLA', 'Emile'),
(44, 'OWONA', 'Zoé'),
(45, 'EBONGUE', 'Serge'),
(46, 'MEKEL', 'Chantal'),
(47, 'NSANGOU', 'Roger'),
(48, 'NJIKI', 'Sandrine'),
(49, 'TCHOUA', 'Martin'),
(50, 'MOUELLE', 'Hélène'),
(51, 'EYENGA', 'Patrick'),
(52, 'BILE', 'Célestine'),
(53, 'OWONA', 'André'),
(54, 'MBOCK', 'Solange'),
(55, 'NKOA', 'Cédric');

-- ── 1. Un user par organisation active ───────────────────────────────────────
INSERT INTO users (
    id, staff_number, email, last_name, first_name, phone, role,
    organization_id, job_title, password_hash,
    must_change_password, failed_login_attempts, locked_until, active,
    substitute_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE(UUID(), '-', '')),
    CONCAT('MAT-', 2008 + ((x.rn - 1) % 17), '-', LPAD(x.rn, 4, '0')),
    CONCAT(
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
            LOWER(n.first_name),
            'é','e'),'è','e'),'ê','e'),'ë','e'),'à','a'),'â','a'),'ä','a'),
            'ô','o'),'ö','o'),'ù','u'),'û','u'),'ü','u'),'ç','c'),
            'ï','i'),'î','i'),'ÿ','y'),'ñ','n'),' ',''),
        '.',
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
            LOWER(n.last_name),
            'é','e'),'è','e'),'ê','e'),'ë','e'),'à','a'),'â','a'),'ä','a'),
            'ô','o'),'ö','o'),'ù','u'),'û','u'),'ü','u'),'ç','c'),
            'ï','i'),'î','i'),'ÿ','y'),'ñ','n'),' ',''),
        '@mintp.cm'
    ),
    n.last_name,
    n.first_name,
    CONCAT('+237 6', LPAD(x.rn, 8, '0')),
    CASE
        WHEN o.code = 'DSI' THEN 'SUPER_ADMIN'
        WHEN o.code = 'MINTP' THEN 'BUSINESS_ADMIN'
        WHEN o.code = 'MINTP-CABINET' THEN 'EXECUTIVE_OFFICE'
        WHEN o.code = 'MINTP-SG' THEN 'SECRETARY_GENERAL'
        WHEN o.code = 'CEL-COM' THEN 'READER'
        WHEN o.code IN ('DAG-COURRIER', 'DAG-ARCHIVES') THEN 'SUPPORT'
        WHEN ot.code = 'REGIONAL_DIRECTORATE' THEN 'REGIONAL_DIRECTOR'
        WHEN ot.code = 'DIRECTORATE' THEN 'DIRECTOR'
        WHEN ot.code = 'DIVISION' THEN 'SERVICE_HEAD'
        ELSE 'AGENT'
    END,
    o.id,
    CONCAT('Responsable - ', o.name),
    @pwd,
    FALSE, 0, NULL, TRUE, NULL, NOW(6), NOW(6)
FROM (
    SELECT
        o.id,
        o.code,
        o.name,
        o.organization_type_id,
        ROW_NUMBER() OVER (ORDER BY o.code) AS rn
    FROM organization o
    WHERE o.active = TRUE
) x
JOIN organization o ON o.id = x.id
JOIN organization_type ot ON ot.id = o.organization_type_id
JOIN seed_cam_names n ON n.seq = x.rn
WHERE NOT EXISTS (
    SELECT 1 FROM users u WHERE u.organization_id = o.id AND u.staff_number LIKE 'MAT-%'
);

-- ── 2. Garantir e.fotso (super admin opérationnel) ───────────────────────────
INSERT INTO users (
    id, staff_number, email, last_name, first_name, phone, role,
    organization_id, job_title, password_hash,
    must_change_password, failed_login_attempts, locked_until, active,
    substitute_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE(UUID(), '-', '')),
    'MAT-2014-0006',
    'e.fotso@mintp.cm',
    'FOTSO',
    'Emmanuel',
    '+237 677 20 10 01',
    'SUPER_ADMIN',
    (SELECT id FROM organization WHERE code = 'DSI' LIMIT 1),
    'Administrateur système',
    @pwd,
    FALSE, 0, NULL, TRUE, NULL, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'e.fotso@mintp.cm');

-- ── 3. Compléter les rôles manquants (au moins 1 user / rôle) ────────────────
INSERT INTO users (
    id, staff_number, email, last_name, first_name, phone, role,
    organization_id, job_title, password_hash,
    must_change_password, failed_login_attempts, locked_until, active,
    substitute_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE(UUID(), '-', '')),
    CONCAT(
        'MAT-2020-',
        LPAD(
            9000 + ROW_NUMBER() OVER (ORDER BY r.name),
            4,
            '0'
        )
    ),
    CONCAT(
        LOWER(CASE r.name
            WHEN 'SUPER_ADMIN' THEN 'paul'
            WHEN 'BUSINESS_ADMIN' THEN 'alice'
            WHEN 'EXECUTIVE_OFFICE' THEN 'bernard'
            WHEN 'SECRETARY_GENERAL' THEN 'claire'
            WHEN 'DIRECTOR' THEN 'daniel'
            WHEN 'SERVICE_HEAD' THEN 'esther'
            WHEN 'AGENT' THEN 'gerard'
            WHEN 'SUPPORT' THEN 'hortense'
            WHEN 'READER' THEN 'isabelle'
            WHEN 'REGIONAL_DIRECTOR' THEN 'jules'
            ELSE 'seed'
        END),
        '.',
        LOWER(CASE r.name
            WHEN 'SUPER_ADMIN' THEN 'nguema'
            WHEN 'BUSINESS_ADMIN' THEN 'kamga'
            WHEN 'EXECUTIVE_OFFICE' THEN 'atangana'
            WHEN 'SECRETARY_GENERAL' THEN 'essomba'
            WHEN 'DIRECTOR' THEN 'mballa'
            WHEN 'SERVICE_HEAD' THEN 'onana'
            WHEN 'AGENT' THEN 'fouda'
            WHEN 'SUPPORT' THEN 'owona'
            WHEN 'READER' THEN 'talla'
            WHEN 'REGIONAL_DIRECTOR' THEN 'ndjock'
            ELSE 'user'
        END),
        '@mintp.cm'
    ),
    CASE r.name
        WHEN 'SUPER_ADMIN' THEN 'NGUEMA'
        WHEN 'BUSINESS_ADMIN' THEN 'KAMGA'
        WHEN 'EXECUTIVE_OFFICE' THEN 'ATANGANA'
        WHEN 'SECRETARY_GENERAL' THEN 'ESSOMBA'
        WHEN 'DIRECTOR' THEN 'MBALLA'
        WHEN 'SERVICE_HEAD' THEN 'ONANA'
        WHEN 'AGENT' THEN 'FOUDA'
        WHEN 'SUPPORT' THEN 'OWONA'
        WHEN 'READER' THEN 'TALLA'
        WHEN 'REGIONAL_DIRECTOR' THEN 'NDJOCK'
        ELSE 'NANA'
    END,
    CASE r.name
        WHEN 'SUPER_ADMIN' THEN 'Paul'
        WHEN 'BUSINESS_ADMIN' THEN 'Alice'
        WHEN 'EXECUTIVE_OFFICE' THEN 'Bernard'
        WHEN 'SECRETARY_GENERAL' THEN 'Claire'
        WHEN 'DIRECTOR' THEN 'Daniel'
        WHEN 'SERVICE_HEAD' THEN 'Esther'
        WHEN 'AGENT' THEN 'Gérard'
        WHEN 'SUPPORT' THEN 'Hortense'
        WHEN 'READER' THEN 'Isabelle'
        WHEN 'REGIONAL_DIRECTOR' THEN 'Jules'
        ELSE 'Seed'
    END,
    '+237 699 00 00 00',
    r.name,
    COALESCE(
        (SELECT id FROM organization WHERE code = 'MINTP' LIMIT 1),
        (SELECT id FROM organization WHERE active = TRUE ORDER BY code LIMIT 1)
    ),
    CONCAT('Couverture rôle ', r.name),
    @pwd,
    FALSE, 0, NULL, TRUE, NULL, NOW(6), NOW(6)
FROM roles r
WHERE r.name IN (
        'SUPER_ADMIN', 'BUSINESS_ADMIN', 'EXECUTIVE_OFFICE', 'SECRETARY_GENERAL',
        'DIRECTOR', 'SERVICE_HEAD', 'AGENT', 'SUPPORT', 'READER', 'REGIONAL_DIRECTOR'
    )
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.role = r.name AND u.active = TRUE)
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = CONCAT('role.', LOWER(r.name), '@mintp.cm'));

-- ── 4. Lier user_roles (RBAC) ────────────────────────────────────────────────
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = u.role
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);

DROP TEMPORARY TABLE seed_cam_names;

-- ── 5. Contrôles ─────────────────────────────────────────────────────────────
SELECT
    (SELECT COUNT(*) FROM organization WHERE active = TRUE) AS active_orgs,
    (SELECT COUNT(DISTINCT organization_id) FROM users WHERE active = TRUE) AS orgs_with_user,
    (SELECT COUNT(*) FROM users WHERE active = TRUE) AS active_users;

SELECT email, last_name, first_name, role
FROM users
WHERE active = TRUE
ORDER BY email
LIMIT 20;
