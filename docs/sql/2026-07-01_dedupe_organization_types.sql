-- Objectif : supprimer les types d'organisation en double (même code), conserver la ligne la plus ancienne
-- Table impactée : organization_type
-- Prérequis : sauvegarde ou environnement de dev ; vérifier les FK organization.organization_type_id avant exécution
-- Exécution : manuelle sur MySQL

-- Vérification des doublons par code :
-- SELECT code, COUNT(*) AS n FROM organization_type GROUP BY code HAVING n > 1;

-- Réassigner les organisations vers le type conservé (id le plus petit par code)
UPDATE organization o
    INNER JOIN organization_type ot_dup ON o.organization_type_id = ot_dup.id
    INNER JOIN (
        SELECT code, MIN(id) AS keep_id
        FROM organization_type
        GROUP BY code
    ) keeper ON keeper.code = ot_dup.code
SET o.organization_type_id = keeper.keep_id
WHERE o.organization_type_id <> keeper.keep_id;

-- Supprimer les doublons
DELETE ot
FROM organization_type ot
    INNER JOIN organization_type ot_keep
        ON ot.code = ot_keep.code AND ot.id > ot_keep.id;

-- rollback : non applicable (suppression destructive)
