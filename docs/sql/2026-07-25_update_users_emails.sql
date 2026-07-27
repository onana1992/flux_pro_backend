-- Objectif : emails dérivés du prénom/nom (ASCII), ex. jean.atangana@mintp.cm
-- Conserve e.fotso@mintp.cm
-- Exécution : mysql --default-character-set=utf8mb4 …

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Table de travail
CREATE TEMPORARY TABLE user_email_map (
    id BINARY(16) PRIMARY KEY,
    local_part VARCHAR(180) CHARACTER SET utf8mb4 NOT NULL
) ENGINE=Memory;

INSERT INTO user_email_map (id, local_part)
SELECT
    u.id,
    CONCAT(
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
            LOWER(u.first_name),
            'é','e'),'è','e'),'ê','e'),'ë','e'),'à','a'),'â','a'),'ä','a'),
            'ô','o'),'ö','o'),'ù','u'),'û','u'),'ü','u'),'ç','c'),
            'ï','i'),'î','i'),'ÿ','y'),'ñ','n'),' ',''),
        '.',
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
            LOWER(u.last_name),
            'é','e'),'è','e'),'ê','e'),'ë','e'),'à','a'),'â','a'),'ä','a'),
            'ô','o'),'ö','o'),'ù','u'),'û','u'),'ü','u'),'ç','c'),
            'ï','i'),'î','i'),'ÿ','y'),'ñ','n'),' ','')
    )
FROM users u
WHERE (u.email LIKE 'u.%@mintp.cm' OR u.email LIKE 'role.%@mintp.cm')
  AND u.email <> 'e.fotso@mintp.cm';

-- Appliquer (avec suffixe si doublon de local_part)
UPDATE users u
INNER JOIN (
    SELECT
        id,
        CONCAT(
            local_part,
            CASE WHEN rn = 1 THEN '' ELSE CAST(rn AS CHAR) END,
            '@mintp.cm'
        ) AS new_email
    FROM (
        SELECT
            id,
            local_part,
            ROW_NUMBER() OVER (PARTITION BY local_part ORDER BY id) AS rn
        FROM user_email_map
    ) x
) m ON m.id = u.id
SET
    u.email = m.new_email,
    u.updated_at = NOW(6);

DROP TEMPORARY TABLE user_email_map;

-- Garantir e.fotso
UPDATE users
SET email = 'e.fotso@mintp.cm', updated_at = NOW(6)
WHERE staff_number = 'MAT-2014-0006'
  AND last_name = 'FOTSO'
  AND first_name = 'Emmanuel';

SELECT email, first_name, last_name, role
FROM users
WHERE active = TRUE
ORDER BY last_name, first_name;
