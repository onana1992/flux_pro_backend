-- Objectif : créer les users manquants (rôle × org des maillons PD) + réassigner
-- Mot de passe : Fluxpro2026@
-- Exécution : mysql --default-character-set=utf8mb4 …

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @pwd := '$2a$12$9VajABCCIisF4Swz2Uh1/.LkyJncWQ7wzo91XbMuI1NspPt2d8wOS';

-- Paires (org, rôle) requises par les maillons des PD sans user actif
CREATE TEMPORARY TABLE needed_pairs (
    organization_id BINARY(16) NOT NULL,
    org_code VARCHAR(64) NOT NULL,
    role VARCHAR(30) NOT NULL,
    PRIMARY KEY (organization_id, role)
) ENGINE=Memory;

INSERT IGNORE INTO needed_pairs (organization_id, org_code, role)
SELECT DISTINCT cst.organization_id, o.code, cst.responsible_role
FROM preconfigured_dossiers pd
JOIN chain_step_templates cst ON cst.chain_template_id = pd.chain_template_id
JOIN organization o ON o.id = cst.organization_id
WHERE pd.chain_template_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM users u
      WHERE u.active = TRUE
        AND u.role = cst.responsible_role
        AND u.organization_id = cst.organization_id
  );

-- Banque de noms
CREATE TEMPORARY TABLE gap_names (
    seq INT PRIMARY KEY,
    last_name VARCHAR(80) NOT NULL,
    first_name VARCHAR(80) NOT NULL
) ENGINE=Memory;

INSERT INTO gap_names (seq, last_name, first_name) VALUES
(1, 'ABANDA', 'Paul'),
(2, 'AKONO', 'Marie'),
(3, 'BAHANE', 'Jean'),
(4, 'BENGONO', 'Sylvie'),
(5, 'DJOM', 'Marcel'),
(6, 'EKENG', 'Grace'),
(7, 'ENOW', 'Alain'),
(8, 'FOMENA', 'Patricia'),
(9, 'KENGNE', 'Joseph'),
(10, 'LONTSI', 'Carine'),
(11, 'MAKOH', 'Bertrand'),
(12, 'MENDO', 'Diane'),
(13, 'NKOT', 'Hervé'),
(14, 'NLOM', 'Lucie'),
(15, 'OBAMA', 'Georges'),
(16, 'PENDA', 'Marie'),
(17, 'SAKWE', 'Eric'),
(18, 'TAMO', 'Florence'),
(19, 'WANDJI', 'Pierre'),
(20, 'YONDO', 'Fatou'),
(21, 'ZEBAZE', 'Nicole'),
(22, 'BIWONG', 'Chantal'),
(23, 'CHAM', 'Samuel'),
(24, 'DEFANG', 'Hélène'),
(25, 'EBAH', 'Jeanne'),
(26, 'FON', 'Olivier'),
(27, 'GUEMESONG', 'Thérèse'),
(28, 'HAPPI', 'Blaise'),
(29, 'IKOME', 'Ingrid'),
(30, 'JOMBE', 'William'),
(31, 'KOME', 'Rachel'),
(32, 'LIBII', 'François'),
(33, 'MOUKOKO', 'Sandrine'),
(34, 'NJOYA', 'Valérie'),
(35, 'OSIM', 'Roger'),
(36, 'POUOKAM', 'Yannick'),
(37, 'SUH', 'Irène'),
(38, 'TANYI', 'Jacques'),
(39, 'UWOYA', 'Gisèle'),
(40, 'VUBANGSI', 'Zoé');

-- Insérer un user par paire manquante
INSERT INTO users (
    id, staff_number, email, last_name, first_name, phone, role,
    organization_id, job_title, password_hash,
    must_change_password, failed_login_attempts, locked_until, active,
    substitute_id, created_at, updated_at
)
SELECT
    UNHEX(REPLACE(UUID(), '-', '')),
    CONCAT(
        'MAT-',
        2010 + ((x.rn - 1) % 14),
        '-',
        LPAD(5000 + x.rn, 4, '0')
    ),
    CONCAT(
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
            LOWER(n.first_name),
            'é','e'),'è','e'),'ê','e'),'ë','e'),'à','a'),'ô','o'),'ù','u'),'ç','c'),
        '.',
        REPLACE(REPLACE(REPLACE(REPLACE(
            LOWER(n.last_name),
            'é','e'),'è','e'),'ô','o'),'ç','c'),
        CASE WHEN x.rn > 1 THEN CAST(x.rn AS CHAR) ELSE '' END,
        '@mintp.cm'
    ),
    n.last_name,
    n.first_name,
    CONCAT('+237 67', LPAD(20000000 + x.rn, 8, '0')),
    x.role,
    x.organization_id,
    CONCAT('Responsable - ', x.org_code, ' (', x.role, ')'),
    @pwd,
    FALSE, 0, NULL, TRUE, NULL, NOW(6), NOW(6)
FROM (
    SELECT
        np.organization_id,
        np.org_code,
        np.role,
        ROW_NUMBER() OVER (ORDER BY np.org_code, np.role) AS rn
    FROM needed_pairs np
) x
JOIN gap_names n ON n.seq = ((x.rn - 1) % 40) + 1
WHERE NOT EXISTS (
    SELECT 1 FROM users u
    WHERE u.organization_id = x.organization_id
      AND u.role = x.role
      AND u.active = TRUE
);

-- RBAC
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = u.role
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- Réassigner step_assignments (préfère rôle+org)
UPDATE preconfigured_dossiers pd
INNER JOIN (
    SELECT
        pd2.id AS pd_id,
        CONCAT(
            '{',
            GROUP_CONCAT(
                CONCAT(
                    '"',
                    LOWER(CONCAT(
                        SUBSTR(HEX(cst.id), 1, 8), '-',
                        SUBSTR(HEX(cst.id), 9, 4), '-',
                        SUBSTR(HEX(cst.id), 13, 4), '-',
                        SUBSTR(HEX(cst.id), 17, 4), '-',
                        SUBSTR(HEX(cst.id), 21, 12)
                    )),
                    '":"',
                    LOWER(CONCAT(
                        SUBSTR(HEX(COALESCE(
                            (SELECT u.id FROM users u
                             WHERE u.active = TRUE
                               AND u.role = cst.responsible_role
                               AND u.organization_id = cst.organization_id
                             ORDER BY u.created_at ASC LIMIT 1),
                            (SELECT u.id FROM users u
                             WHERE u.active = TRUE
                               AND u.organization_id = cst.organization_id
                             ORDER BY u.created_at ASC LIMIT 1),
                            (SELECT u.id FROM users u
                             WHERE u.active = TRUE AND u.role = cst.responsible_role
                             ORDER BY u.created_at ASC LIMIT 1)
                        )), 1, 8), '-',
                        SUBSTR(HEX(COALESCE(
                            (SELECT u.id FROM users u
                             WHERE u.active = TRUE
                               AND u.role = cst.responsible_role
                               AND u.organization_id = cst.organization_id
                             ORDER BY u.created_at ASC LIMIT 1),
                            (SELECT u.id FROM users u
                             WHERE u.active = TRUE
                               AND u.organization_id = cst.organization_id
                             ORDER BY u.created_at ASC LIMIT 1),
                            (SELECT u.id FROM users u
                             WHERE u.active = TRUE AND u.role = cst.responsible_role
                             ORDER BY u.created_at ASC LIMIT 1)
                        )), 9, 4), '-',
                        SUBSTR(HEX(COALESCE(
                            (SELECT u.id FROM users u
                             WHERE u.active = TRUE
                               AND u.role = cst.responsible_role
                               AND u.organization_id = cst.organization_id
                             ORDER BY u.created_at ASC LIMIT 1),
                            (SELECT u.id FROM users u
                             WHERE u.active = TRUE
                               AND u.organization_id = cst.organization_id
                             ORDER BY u.created_at ASC LIMIT 1),
                            (SELECT u.id FROM users u
                             WHERE u.active = TRUE AND u.role = cst.responsible_role
                             ORDER BY u.created_at ASC LIMIT 1)
                        )), 13, 4), '-',
                        SUBSTR(HEX(COALESCE(
                            (SELECT u.id FROM users u
                             WHERE u.active = TRUE
                               AND u.role = cst.responsible_role
                               AND u.organization_id = cst.organization_id
                             ORDER BY u.created_at ASC LIMIT 1),
                            (SELECT u.id FROM users u
                             WHERE u.active = TRUE
                               AND u.organization_id = cst.organization_id
                             ORDER BY u.created_at ASC LIMIT 1),
                            (SELECT u.id FROM users u
                             WHERE u.active = TRUE AND u.role = cst.responsible_role
                             ORDER BY u.created_at ASC LIMIT 1)
                        )), 17, 4), '-',
                        SUBSTR(HEX(COALESCE(
                            (SELECT u.id FROM users u
                             WHERE u.active = TRUE
                               AND u.role = cst.responsible_role
                               AND u.organization_id = cst.organization_id
                             ORDER BY u.created_at ASC LIMIT 1),
                            (SELECT u.id FROM users u
                             WHERE u.active = TRUE
                               AND u.organization_id = cst.organization_id
                             ORDER BY u.created_at ASC LIMIT 1),
                            (SELECT u.id FROM users u
                             WHERE u.active = TRUE AND u.role = cst.responsible_role
                             ORDER BY u.created_at ASC LIMIT 1)
                        )), 21, 12)
                    )),
                    '"'
                )
                ORDER BY cst.step_order, cst.id
                SEPARATOR ','
            ),
            '}'
        ) AS assignments
    FROM preconfigured_dossiers pd2
    JOIN chain_step_templates cst ON cst.chain_template_id = pd2.chain_template_id
    WHERE pd2.chain_template_id IS NOT NULL
    GROUP BY pd2.id
) x ON x.pd_id = pd.id
SET
    pd.step_assignments = x.assignments,
    pd.updated_at = NOW(6);

-- Contrôles
SELECT 'pairs_needed' AS k, COUNT(*) AS n FROM needed_pairs
UNION ALL
SELECT 'users_total', COUNT(*) FROM users WHERE active = TRUE
UNION ALL
SELECT 'gaps_remaining', COUNT(*) FROM (
    SELECT 1
    FROM preconfigured_dossiers pd
    JOIN chain_step_templates cst ON cst.chain_template_id = pd.chain_template_id
    WHERE pd.chain_template_id IS NOT NULL
      AND NOT EXISTS (
          SELECT 1 FROM users u
          WHERE u.active = TRUE
            AND u.role = cst.responsible_role
            AND u.organization_id = cst.organization_id
      )
) g;

SELECT pd.code, cst.step_order, cst.label, cst.responsible_role, o.code AS org,
       (SELECT CONCAT(u.first_name, ' ', u.last_name)
        FROM users u
        WHERE u.active = TRUE AND u.role = cst.responsible_role AND u.organization_id = cst.organization_id
        ORDER BY u.created_at LIMIT 1) AS assignee
FROM preconfigured_dossiers pd
JOIN chain_step_templates cst ON cst.chain_template_id = pd.chain_template_id
JOIN organization o ON o.id = cst.organization_id
WHERE pd.chain_template_id IS NOT NULL
ORDER BY pd.code, cst.step_order;

DROP TEMPORARY TABLE needed_pairs;
DROP TEMPORARY TABLE gap_names;
