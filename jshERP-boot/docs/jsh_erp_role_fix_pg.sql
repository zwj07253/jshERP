BEGIN;

-- 保留重复角色及其业务关系，通过重命名消除同租户活动角色重名。
WITH duplicate_roles AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY COALESCE(tenant_id, 0), name
               ORDER BY id
           ) AS row_number
    FROM jsh_role
    WHERE COALESCE(delete_flag, '0') != '1'
)
UPDATE jsh_role role
SET name = LEFT(role.name, 25) || '_dup_' || role.id
FROM duplicate_roles duplicate
WHERE role.id = duplicate.id
  AND duplicate.row_number > 1;

-- 同一租户、类型和主键只保留最新一条活动关系，旧记录逻辑删除。
WITH duplicate_relations AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY COALESCE(tenant_id, 0), type, key_id
               ORDER BY id DESC
           ) AS row_number
    FROM jsh_user_business
    WHERE COALESCE(delete_flag, '0') != '1'
)
UPDATE jsh_user_business relation
SET delete_flag = '1'
FROM duplicate_relations duplicate
WHERE relation.id = duplicate.id
  AND duplicate.row_number > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_role_tenant_name_active
    ON jsh_role(COALESCE(tenant_id, 0), name)
    WHERE COALESCE(delete_flag, '0') != '1';

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_business_relation_active
    ON jsh_user_business(COALESCE(tenant_id, 0), type, key_id)
    WHERE COALESCE(delete_flag, '0') != '1';

COMMIT;
