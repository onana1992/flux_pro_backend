-- Objectif : responsables de tous les maillons (JSON step_assignments)
-- Format : {"<stepUuid>":"<userUuid>", ...}

ALTER TABLE preconfigured_dossiers
    ADD COLUMN step_assignments LONGTEXT NULL AFTER chain_template_id;

-- Helper : BINARY(16) → UUID string
-- Migrer RH-CONGE : tous les maillons T-CONGE avec un user du rôle du maillon

SET @tpl_id := (SELECT id FROM chain_templates WHERE code = 'T-CONGE' LIMIT 1);
SET @pd_id := (SELECT id FROM preconfigured_dossiers WHERE code = 'RH-CONGE' LIMIT 1);

SET @s1 := (SELECT id FROM chain_step_templates WHERE chain_template_id = @tpl_id AND step_order = 1 LIMIT 1);
SET @s2 := (SELECT id FROM chain_step_templates WHERE chain_template_id = @tpl_id AND step_order = 2 LIMIT 1);
SET @s3 := (SELECT id FROM chain_step_templates WHERE chain_template_id = @tpl_id AND step_order = 3 LIMIT 1);

SET @u1 := COALESCE(
    (SELECT default_first_step_responsible_user_id FROM preconfigured_dossiers WHERE id = @pd_id),
    (SELECT id FROM users WHERE role = 'SERVICE_HEAD' AND active = TRUE ORDER BY created_at ASC LIMIT 1)
);
SET @u2 := (SELECT id FROM users WHERE role = 'DIRECTOR' AND active = TRUE ORDER BY created_at ASC LIMIT 1);
SET @u3 := COALESCE(
    @u1,
    (SELECT id FROM users WHERE role = 'SERVICE_HEAD' AND active = TRUE ORDER BY created_at ASC LIMIT 1)
);

-- uuid_bin_to_str
SET @s1s := LOWER(CONCAT(
    SUBSTR(HEX(@s1),1,8),'-',SUBSTR(HEX(@s1),9,4),'-',SUBSTR(HEX(@s1),13,4),'-',
    SUBSTR(HEX(@s1),17,4),'-',SUBSTR(HEX(@s1),21,12)));
SET @s2s := LOWER(CONCAT(
    SUBSTR(HEX(@s2),1,8),'-',SUBSTR(HEX(@s2),9,4),'-',SUBSTR(HEX(@s2),13,4),'-',
    SUBSTR(HEX(@s2),17,4),'-',SUBSTR(HEX(@s2),21,12)));
SET @s3s := LOWER(CONCAT(
    SUBSTR(HEX(@s3),1,8),'-',SUBSTR(HEX(@s3),9,4),'-',SUBSTR(HEX(@s3),13,4),'-',
    SUBSTR(HEX(@s3),17,4),'-',SUBSTR(HEX(@s3),21,12)));
SET @u1s := LOWER(CONCAT(
    SUBSTR(HEX(@u1),1,8),'-',SUBSTR(HEX(@u1),9,4),'-',SUBSTR(HEX(@u1),13,4),'-',
    SUBSTR(HEX(@u1),17,4),'-',SUBSTR(HEX(@u1),21,12)));
SET @u2s := LOWER(CONCAT(
    SUBSTR(HEX(@u2),1,8),'-',SUBSTR(HEX(@u2),9,4),'-',SUBSTR(HEX(@u2),13,4),'-',
    SUBSTR(HEX(@u2),17,4),'-',SUBSTR(HEX(@u2),21,12)));
SET @u3s := LOWER(CONCAT(
    SUBSTR(HEX(@u3),1,8),'-',SUBSTR(HEX(@u3),9,4),'-',SUBSTR(HEX(@u3),13,4),'-',
    SUBSTR(HEX(@u3),17,4),'-',SUBSTR(HEX(@u3),21,12)));

UPDATE preconfigured_dossiers
SET step_assignments = JSON_OBJECT(@s1s, @u1s, @s2s, @u2s, @s3s, @u3s),
    updated_at = NOW(6)
WHERE id = @pd_id
  AND @s1 IS NOT NULL AND @u1 IS NOT NULL AND @u2 IS NOT NULL;

SELECT code, step_assignments FROM preconfigured_dossiers WHERE code = 'RH-CONGE';
