-- Apply this once to an already-created foreign trade Demo database.
-- It creates the documented jsh / 123456 account and assigns administrator permissions.

INSERT INTO jsh_user (id, username, login_name, password, leader_flag, ismanager, isystem, status, description, tenant_id, delete_flag)
VALUES (101, 'Demo Administrator', 'jsh', 'e10adc3949ba59abbe56e057f20f883e', '1', 1, 0, 0, 'Foreign trade demo account', 0, '0')
ON CONFLICT (id) DO UPDATE SET
username = EXCLUDED.username,
login_name = EXCLUDED.login_name,
password = EXCLUDED.password,
leader_flag = EXCLUDED.leader_flag,
ismanager = EXCLUDED.ismanager,
isystem = EXCLUDED.isystem,
status = EXCLUDED.status,
description = EXCLUDED.description,
tenant_id = EXCLUDED.tenant_id,
delete_flag = EXCLUDED.delete_flag;

INSERT INTO jsh_user_business (id, type, key_id, value, btn_str, tenant_id, delete_flag)
VALUES (113, 'UserRole', '101', '[4]', NULL, 0, '0')
ON CONFLICT (id) DO UPDATE SET
type = EXCLUDED.type,
key_id = EXCLUDED.key_id,
value = EXCLUDED.value,
btn_str = EXCLUDED.btn_str,
tenant_id = EXCLUDED.tenant_id,
delete_flag = EXCLUDED.delete_flag;

INSERT INTO jsh_user_business (id, type, key_id, value, btn_str, tenant_id, delete_flag)
VALUES (114, 'UserRole', '0', '[4]', NULL, 0, '0')
ON CONFLICT (id) DO UPDATE SET
type = EXCLUDED.type,
key_id = EXCLUDED.key_id,
value = EXCLUDED.value,
btn_str = EXCLUDED.btn_str,
tenant_id = EXCLUDED.tenant_id,
delete_flag = EXCLUDED.delete_flag;

UPDATE jsh_user_business
SET value = value || '[300][301][302][303][304][305]'
WHERE tenant_id = 0 AND type = 'RoleFunctions' AND key_id = '4' AND value NOT LIKE '%[300]%';

UPDATE jsh_user_business
SET btn_str = CASE
    WHEN COALESCE(TRIM(btn_str), '') = '' THEN '[{"funId":302,"btnStr":"1,2,3"},{"funId":303,"btnStr":"1,2,3"}]'
    ELSE LEFT(TRIM(btn_str), LENGTH(TRIM(btn_str)) - 1)
         || ',{"funId":302,"btnStr":"1,2,3"},{"funId":303,"btnStr":"1,2,3"}]'
END
WHERE tenant_id = 0 AND type = 'RoleFunctions' AND key_id = '4' AND COALESCE(btn_str, '') NOT LIKE '%"funId":302%';
