BEGIN;

LOCK TABLE jsh_in_out_item IN SHARE ROW EXCLUSIVE MODE;
LOCK TABLE jsh_account_item IN SHARE ROW EXCLUSIVE MODE;

UPDATE jsh_in_out_item ioi
SET tenant_id = source.tenant_id
FROM (
    SELECT in_out_item_id, MIN(tenant_id) AS tenant_id
    FROM jsh_account_item
    WHERE in_out_item_id IS NOT NULL
    GROUP BY in_out_item_id
) source
WHERE ioi.id = source.in_out_item_id
  AND ioi.tenant_id IS NULL;

UPDATE jsh_in_out_item
SET name = CASE
        WHEN btrim(COALESCE(name, '')) = '' THEN '收支项目-' || id
        ELSE left(btrim(name), 50)
    END,
    type = btrim(type),
    remark = NULLIF(left(btrim(COALESCE(remark, '')), 100), ''),
    sort = CASE WHEN btrim(COALESCE(sort, '')) ~ '^[0-9]{1,10}$'
                THEN (btrim(sort)::bigint)::text ELSE NULL END,
    enabled = COALESCE(enabled, TRUE),
    tenant_id = COALESCE(tenant_id, 0),
    delete_flag = COALESCE(delete_flag, '0');

WITH inferred_type AS (
    SELECT ai.in_out_item_id,
           CASE WHEN COUNT(DISTINCT ah.type) = 1 THEN MIN(ah.type) END AS type
    FROM jsh_account_item ai
    JOIN jsh_account_head ah ON ah.id = ai.header_id
    WHERE COALESCE(ai.delete_flag, '0') <> '1'
      AND COALESCE(ah.delete_flag, '0') <> '1'
      AND ah.type IN ('收入', '支出')
    GROUP BY ai.in_out_item_id
)
UPDATE jsh_in_out_item ioi
SET type = inferred_type.type
FROM inferred_type
WHERE ioi.id = inferred_type.in_out_item_id
  AND (ioi.type IS NULL OR ioi.type NOT IN ('收入', '支出'))
  AND inferred_type.type IS NOT NULL;

UPDATE jsh_in_out_item ioi
SET type = '支出', delete_flag = '1'
WHERE (ioi.type IS NULL OR ioi.type NOT IN ('收入', '支出'))
  AND NOT EXISTS (
      SELECT 1 FROM jsh_account_item ai
      WHERE ai.in_out_item_id = ioi.id
        AND COALESCE(ai.delete_flag, '0') <> '1'
  );

DO $$
DECLARE
    mismatch RECORD;
    target_id BIGINT;
BEGIN
    FOR mismatch IN
        SELECT DISTINCT ioi.id AS source_id, ioi.tenant_id, ioi.name,
               ioi.remark, ioi.enabled, ioi.sort, ah.type AS target_type
        FROM jsh_in_out_item ioi
        JOIN jsh_account_item ai ON ai.in_out_item_id = ioi.id
        JOIN jsh_account_head ah ON ah.id = ai.header_id
        WHERE COALESCE(ioi.delete_flag, '0') <> '1'
          AND COALESCE(ai.delete_flag, '0') <> '1'
          AND COALESCE(ah.delete_flag, '0') <> '1'
          AND ah.type IN ('收入', '支出')
          AND ioi.type IS DISTINCT FROM ah.type
    LOOP
        SELECT MIN(id) INTO target_id
        FROM jsh_in_out_item
        WHERE tenant_id = mismatch.tenant_id
          AND type = mismatch.target_type
          AND name = mismatch.name
          AND COALESCE(delete_flag, '0') <> '1';

        IF target_id IS NULL THEN
            INSERT INTO jsh_in_out_item(name, type, remark, enabled, sort, tenant_id, delete_flag)
            VALUES (mismatch.name, mismatch.target_type, mismatch.remark,
                    mismatch.enabled, mismatch.sort, mismatch.tenant_id, '0')
            RETURNING id INTO target_id;
        END IF;

        UPDATE jsh_account_item ai
        SET in_out_item_id = target_id
        FROM jsh_account_head ah
        WHERE ai.header_id = ah.id
          AND ai.in_out_item_id = mismatch.source_id
          AND ah.type = mismatch.target_type
          AND COALESCE(ai.delete_flag, '0') <> '1'
          AND COALESCE(ah.delete_flag, '0') <> '1';
    END LOOP;
END $$;

WITH ranked AS (
    SELECT id,
           MIN(id) OVER (PARTITION BY tenant_id, type, name) AS keep_id,
           ROW_NUMBER() OVER (PARTITION BY tenant_id, type, name ORDER BY id) AS row_num
    FROM jsh_in_out_item
    WHERE delete_flag <> '1'
), duplicates AS (
    SELECT id, keep_id FROM ranked WHERE row_num > 1
)
UPDATE jsh_account_item ai
SET in_out_item_id = duplicates.keep_id
FROM duplicates
WHERE ai.in_out_item_id = duplicates.id;

WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY tenant_id, type, name ORDER BY id) AS row_num
    FROM jsh_in_out_item
    WHERE delete_flag <> '1'
)
UPDATE jsh_in_out_item ioi
SET delete_flag = '1'
FROM ranked
WHERE ioi.id = ranked.id
  AND ranked.row_num > 1;

UPDATE jsh_in_out_item
SET type = '支出', delete_flag = '1'
WHERE type IS NULL OR type NOT IN ('收入', '支出');

ALTER TABLE jsh_in_out_item ALTER COLUMN name SET NOT NULL;
ALTER TABLE jsh_in_out_item ALTER COLUMN type SET NOT NULL;
ALTER TABLE jsh_in_out_item ALTER COLUMN enabled SET DEFAULT TRUE;
ALTER TABLE jsh_in_out_item ALTER COLUMN enabled SET NOT NULL;
ALTER TABLE jsh_in_out_item ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE jsh_in_out_item ALTER COLUMN delete_flag SET DEFAULT '0';
ALTER TABLE jsh_in_out_item ALTER COLUMN delete_flag SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'chk_in_out_item_type'
                     AND conrelid = 'jsh_in_out_item'::regclass) THEN
        ALTER TABLE jsh_in_out_item ADD CONSTRAINT chk_in_out_item_type
            CHECK (type IN ('收入', '支出'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'chk_in_out_item_sort'
                     AND conrelid = 'jsh_in_out_item'::regclass) THEN
        ALTER TABLE jsh_in_out_item ADD CONSTRAINT chk_in_out_item_sort
            CHECK (sort IS NULL OR sort ~ '^[0-9]{1,10}$');
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_in_out_item_active_name
    ON jsh_in_out_item(tenant_id, type, name)
    WHERE delete_flag <> '1';

COMMIT;
