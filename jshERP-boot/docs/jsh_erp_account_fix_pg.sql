-- 结算账户历史/测试数据整理。
-- 保留所有记录，只修正空名称、租户内重名和无效默认状态，然后建立一致性约束。

BEGIN;

UPDATE jsh_account SET delete_flag = '0' WHERE delete_flag IS NULL;
UPDATE jsh_account SET enabled = TRUE WHERE delete_flag != '1' AND enabled IS NULL;
UPDATE jsh_account SET initial_amount = 0 WHERE initial_amount IS NULL;

UPDATE jsh_account
SET name = '结算账户-' || id
WHERE COALESCE(delete_flag, '0') != '1' AND BTRIM(COALESCE(name, '')) = '';

WITH duplicate_accounts AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY COALESCE(tenant_id, 0), name ORDER BY id) AS row_number
    FROM jsh_account
    WHERE COALESCE(delete_flag, '0') != '1'
)
UPDATE jsh_account account
SET name = LEFT(account.name, 25) || '-' || account.id || '-' || SUBSTRING(MD5(account.id::text), 1, 8)
FROM duplicate_accounts duplicate
WHERE account.id = duplicate.id AND duplicate.row_number > 1;

UPDATE jsh_account
SET is_default = FALSE
WHERE COALESCE(delete_flag, '0') = '1' OR enabled IS NOT TRUE;

WITH ranked_accounts AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY COALESCE(tenant_id, 0)
               ORDER BY CASE WHEN is_default IS TRUE THEN 0 ELSE 1 END, id
           ) AS row_number
    FROM jsh_account
    WHERE COALESCE(delete_flag, '0') != '1' AND enabled IS TRUE
)
UPDATE jsh_account account
SET is_default = (ranked.row_number = 1)
FROM ranked_accounts ranked
WHERE account.id = ranked.id;

CREATE UNIQUE INDEX IF NOT EXISTS uk_account_tenant_name_active
ON jsh_account(COALESCE(tenant_id, 0), name)
WHERE COALESCE(delete_flag, '0') != '1';

CREATE UNIQUE INDEX IF NOT EXISTS uk_account_tenant_default_active
ON jsh_account(COALESCE(tenant_id, 0))
WHERE COALESCE(delete_flag, '0') != '1' AND is_default IS TRUE;

COMMIT;
