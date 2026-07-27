-- Objectif : step_assignments pour dossiers préconfigurés (portail)
-- Préfère user actif du même rôle DANS l'organisation du maillon
-- Fallback : même org → même rôle → 1er user actif
-- Idempotent

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
                             ORDER BY u.created_at ASC LIMIT 1),
                            (SELECT u.id FROM users u
                             WHERE u.active = TRUE
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
                             ORDER BY u.created_at ASC LIMIT 1),
                            (SELECT u.id FROM users u
                             WHERE u.active = TRUE
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
                             ORDER BY u.created_at ASC LIMIT 1),
                            (SELECT u.id FROM users u
                             WHERE u.active = TRUE
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
                             ORDER BY u.created_at ASC LIMIT 1),
                            (SELECT u.id FROM users u
                             WHERE u.active = TRUE
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
                             ORDER BY u.created_at ASC LIMIT 1),
                            (SELECT u.id FROM users u
                             WHERE u.active = TRUE
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

SELECT
    code,
    portal_audience,
    CASE WHEN step_assignments IS NULL OR step_assignments = '' THEN 0 ELSE 1 END AS has_assignments,
    CHAR_LENGTH(step_assignments) AS json_len
FROM preconfigured_dossiers
ORDER BY code;
