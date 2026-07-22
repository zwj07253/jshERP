-- 用户管理安全与数据完整性修复（PostgreSQL）
-- 可重复执行。保留测试数据；重复的有效登录名会保留最早一条，其余记录改名并软删除。
BEGIN;

LOCK TABLE jsh_user IN SHARE ROW EXCLUSIVE MODE;

WITH duplicate_users AS (
    SELECT id, login_name,
           row_number() OVER (PARTITION BY login_name ORDER BY id) AS row_no
    FROM jsh_user
    WHERE COALESCE(delete_flag, '0') != '1'
)
UPDATE jsh_user u
SET login_name = left(u.login_name, 230) || '_duplicate_' || u.id,
    status = 2,
    delete_flag = '1'
FROM duplicate_users d
WHERE u.id = d.id AND d.row_no > 1;

UPDATE jsh_user SET status = 2 WHERE status IS NULL OR status NOT IN (0, 2);
UPDATE jsh_user SET leader_flag = '0' WHERE leader_flag IS NULL OR leader_flag NOT IN ('0', '1');

ALTER TABLE jsh_user ALTER COLUMN password TYPE VARCHAR(100);

CREATE UNIQUE INDEX IF NOT EXISTS uk_jsh_user_active_login_name
    ON jsh_user (login_name) WHERE COALESCE(delete_flag, '0') != '1';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_jsh_user_status') THEN
        ALTER TABLE jsh_user ADD CONSTRAINT ck_jsh_user_status CHECK (status IN (0, 2));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_jsh_user_leader_flag') THEN
        ALTER TABLE jsh_user ADD CONSTRAINT ck_jsh_user_leader_flag CHECK (leader_flag IN ('0', '1'));
    END IF;
END $$;

UPDATE jsh_orga_user_rel rel
SET delete_flag = '1'
WHERE COALESCE(rel.delete_flag, '0') != '1'
  AND NOT EXISTS (
      SELECT 1 FROM jsh_user u
      WHERE u.id = rel.user_id AND COALESCE(u.delete_flag, '0') != '1'
  );

UPDATE jsh_user_business ub
SET delete_flag = '1'
WHERE COALESCE(ub.delete_flag, '0') != '1'
  AND ub.type IN ('UserRole', 'UserCustomer', 'UserDepot')
  AND CASE
      WHEN ub.key_id ~ '^[0-9]+$' THEN NOT EXISTS (
          SELECT 1 FROM jsh_user u
          WHERE u.id = ub.key_id::BIGINT AND COALESCE(u.delete_flag, '0') != '1'
      )
      ELSE TRUE
  END;

COMMIT;
