-- Objectif : noms/prénoms camerounais typiques + correction encodage (accents / tirets)
-- Prérequis : users seed (emails u.*@mintp.cm, e.fotso, role.*)
-- Mot de passe inchangé : Fluxpro2026@
-- Exécution : mysql --default-character-set=utf8mb4 …

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

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

-- ── 1. Utilisateurs par organisation (emails u.*) ────────────────────────────
UPDATE users u
INNER JOIN (
    SELECT
        u2.id AS user_id,
        o.name AS org_name,
        ROW_NUMBER() OVER (ORDER BY o.code) AS rn
    FROM users u2
    INNER JOIN organization o ON o.id = u2.organization_id
    WHERE u2.staff_number LIKE 'MAT-%'
      AND u2.email <> 'e.fotso@mintp.cm'
      AND u2.job_title LIKE 'Responsable - %'
) x ON x.user_id = u.id
INNER JOIN seed_cam_names n ON n.seq = x.rn
SET
    u.last_name = n.last_name,
    u.first_name = n.first_name,
    u.job_title = CONCAT('Responsable - ', x.org_name),
    u.updated_at = NOW(6);

-- ── 2. Super admin opérationnel ──────────────────────────────────────────────
UPDATE users
SET
    last_name = 'FOTSO',
    first_name = 'Emmanuel',
    job_title = 'Administrateur système',
    updated_at = NOW(6)
WHERE email = 'e.fotso@mintp.cm';

-- ── 3. Couverture rôles (si présents) ────────────────────────────────────────
UPDATE users SET last_name = 'NGUEMA', first_name = 'Paul', job_title = 'Couverture rôle SUPER_ADMIN', updated_at = NOW(6)
WHERE email = 'role.super_admin@mintp.cm';
UPDATE users SET last_name = 'KAMGA', first_name = 'Alice', job_title = 'Couverture rôle BUSINESS_ADMIN', updated_at = NOW(6)
WHERE email = 'role.business_admin@mintp.cm';
UPDATE users SET last_name = 'ATANGANA', first_name = 'Bernard', job_title = 'Couverture rôle EXECUTIVE_OFFICE', updated_at = NOW(6)
WHERE email = 'role.executive_office@mintp.cm';
UPDATE users SET last_name = 'ESSOMBA', first_name = 'Claire', job_title = 'Couverture rôle SECRETARY_GENERAL', updated_at = NOW(6)
WHERE email = 'role.secretary_general@mintp.cm';
UPDATE users SET last_name = 'MBALLA', first_name = 'Daniel', job_title = 'Couverture rôle DIRECTOR', updated_at = NOW(6)
WHERE email = 'role.director@mintp.cm';
UPDATE users SET last_name = 'ONANA', first_name = 'Esther', job_title = 'Couverture rôle SERVICE_HEAD', updated_at = NOW(6)
WHERE email = 'role.service_head@mintp.cm';
UPDATE users SET last_name = 'FOUDA', first_name = 'Gérard', job_title = 'Couverture rôle AGENT', updated_at = NOW(6)
WHERE email = 'role.agent@mintp.cm';
UPDATE users SET last_name = 'OWONA', first_name = 'Hortense', job_title = 'Couverture rôle SUPPORT', updated_at = NOW(6)
WHERE email = 'role.support@mintp.cm';
UPDATE users SET last_name = 'TALLA', first_name = 'Isabelle', job_title = 'Couverture rôle READER', updated_at = NOW(6)
WHERE email = 'role.reader@mintp.cm';
UPDATE users SET last_name = 'NDJOCK', first_name = 'Jules', job_title = 'Couverture rôle REGIONAL_DIRECTOR', updated_at = NOW(6)
WHERE email = 'role.regional_director@mintp.cm';

-- ── 4. Corriger tirets mojibake restants dans job_title ───────────────────────
UPDATE users
SET job_title = REPLACE(REPLACE(job_title, 'ÔÇö', ' - '), '—', ' - '),
    updated_at = NOW(6)
WHERE job_title LIKE '%ÔÇö%' OR job_title LIKE '%—%';

DROP TEMPORARY TABLE seed_cam_names;

SELECT email, last_name, first_name, role, job_title
FROM users
WHERE active = TRUE
ORDER BY email
LIMIT 25;
