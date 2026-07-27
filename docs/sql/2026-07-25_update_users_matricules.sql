-- Objectif : matricules réalistes MAT-AAAA-NNNN (sans préfixe SEED)
-- Exécution : mysql --default-character-set=utf8mb4 …

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Utilisateurs par organisation
UPDATE users u
INNER JOIN (
    SELECT id, ROW_NUMBER() OVER (ORDER BY email) AS rn
    FROM users
    WHERE staff_number LIKE 'MAT-%'
      AND email <> 'e.fotso@mintp.cm'
      AND email LIKE '%@mintp.cm'
      AND job_title LIKE 'Responsable - %'
) x ON x.id = u.id
SET
    u.staff_number = CONCAT(
        'MAT-',
        2008 + ((x.rn - 1) % 17),
        '-',
        LPAD(x.rn, 4, '0')
    ),
    u.updated_at = NOW(6);

-- Super admin opérationnel
UPDATE users
SET staff_number = 'MAT-2014-0006', updated_at = NOW(6)
WHERE email = 'e.fotso@mintp.cm';

-- Couverture rôles (si présents)
UPDATE users u
INNER JOIN (
    SELECT id, ROW_NUMBER() OVER (ORDER BY email) AS rn
    FROM users
    WHERE email LIKE 'role.%@mintp.cm'
) x ON x.id = u.id
SET
    u.staff_number = CONCAT('MAT-2020-', LPAD(9000 + x.rn, 4, '0')),
    u.updated_at = NOW(6);

SELECT email, staff_number, last_name, first_name
FROM users
WHERE active = TRUE
ORDER BY staff_number
LIMIT 25;
