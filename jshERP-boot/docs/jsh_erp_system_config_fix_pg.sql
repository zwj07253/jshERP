-- 系统配置模块安全加固：数据库约束修复脚本（PostgreSQL）
-- 可重复执行，使用 IF NOT EXISTS 避免重复创建。

-- 1. 平台参数：platform_key 唯一索引（防止重复 key）
CREATE UNIQUE INDEX IF NOT EXISTS uk_platform_config_key
    ON jsh_platform_config(platform_key);

-- 2. 系统参数：清理同一租户的重复配置（保留最小 id）
DELETE FROM jsh_system_config a
USING jsh_system_config b
WHERE a.tenant_id = b.tenant_id
  AND a.id > b.id;

-- 3. 系统参数：清理软删除记录（系统配置为单记录表，软删除无意义）
DELETE FROM jsh_system_config WHERE COALESCE(delete_flag, '0') = '1';

-- 4. 系统参数：tenant_id 唯一索引（保证每租户一条配置）
CREATE UNIQUE INDEX IF NOT EXISTS uk_system_config_tenant_id
    ON jsh_system_config(tenant_id);
