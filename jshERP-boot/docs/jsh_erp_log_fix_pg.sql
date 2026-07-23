-- 日志管理安全与数据完整性修复（PostgreSQL）
-- 默认保留最近 180 天；应用启动后会按配置 jsh.log.retention-days 持续清理。
BEGIN;

LOCK TABLE jsh_log IN SHARE ROW EXCLUSIVE MODE;

UPDATE jsh_log l
SET tenant_id = COALESCE(u.tenant_id, 0)
FROM jsh_user u
WHERE l.user_id = u.id
  AND l.tenant_id IS DISTINCT FROM COALESCE(u.tenant_id, 0);

UPDATE jsh_log
SET tenant_id = COALESCE(tenant_id, 0),
    operation = LEFT(COALESCE(NULLIF(BTRIM(operation), ''), '未知操作'), 500),
    client_ip = LEFT(COALESCE(NULLIF(BTRIM(client_ip), ''), 'unknown'), 200),
    create_time = COALESCE(create_time, CURRENT_TIMESTAMP),
    status = CASE WHEN status IN (0, 1) THEN status ELSE 1 END,
    content = LEFT(COALESCE(content, ''), 5000);

ALTER TABLE jsh_log
    ALTER COLUMN operation SET DEFAULT '未知操作',
    ALTER COLUMN operation SET NOT NULL,
    ALTER COLUMN client_ip SET DEFAULT 'unknown',
    ALTER COLUMN client_ip SET NOT NULL,
    ALTER COLUMN create_time SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN create_time SET NOT NULL,
    ALTER COLUMN status SET DEFAULT 0,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN content SET DEFAULT '',
    ALTER COLUMN content SET NOT NULL,
    ALTER COLUMN tenant_id SET DEFAULT 0,
    ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE jsh_log DROP CONSTRAINT IF EXISTS ck_log_status;
ALTER TABLE jsh_log ADD CONSTRAINT ck_log_status CHECK (status IN (0, 1));

CREATE INDEX IF NOT EXISTS idx_log_tenant_create_time
    ON jsh_log(tenant_id, create_time DESC);

DELETE FROM jsh_log
WHERE create_time < CURRENT_TIMESTAMP - INTERVAL '180 days';

COMMIT;
