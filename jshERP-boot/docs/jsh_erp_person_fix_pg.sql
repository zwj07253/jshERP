BEGIN;

LOCK TABLE jsh_person IN SHARE ROW EXCLUSIVE MODE;
LOCK TABLE jsh_account_head IN SHARE ROW EXCLUSIVE MODE;
LOCK TABLE jsh_depot_head IN SHARE ROW EXCLUSIVE MODE;

UPDATE jsh_person p
SET tenant_id = source.tenant_id
FROM (
    SELECT hands_person_id AS person_id, MIN(tenant_id) AS tenant_id
    FROM jsh_account_head
    WHERE hands_person_id IS NOT NULL
    GROUP BY hands_person_id
) source
WHERE p.id = source.person_id AND p.tenant_id IS NULL;

UPDATE jsh_person
SET name = CASE WHEN btrim(COALESCE(name, '')) = '' THEN '经手人-' || id
                ELSE left(btrim(name), 50) END,
    type = CASE btrim(COALESCE(type, ''))
               WHEN '业务员' THEN '销售员'
               WHEN '销售人员' THEN '销售员'
               WHEN '财务人员' THEN '财务员'
               WHEN '仓库管理员' THEN '仓管员'
               ELSE btrim(type) END,
    sort = CASE WHEN btrim(COALESCE(sort, '')) ~ '^[0-9]{1,10}$'
                THEN (btrim(sort)::bigint)::text ELSE NULL END,
    enabled = COALESCE(enabled, TRUE),
    tenant_id = COALESCE(tenant_id, 0),
    delete_flag = COALESCE(delete_flag, '0');

UPDATE jsh_person p
SET delete_flag = '0'
WHERE EXISTS (
          SELECT 1 FROM jsh_account_head ah
          WHERE COALESCE(ah.delete_flag, '0') <> '1' AND ah.hands_person_id = p.id
      )
   OR EXISTS (
          SELECT 1 FROM jsh_depot_head dh
          WHERE COALESCE(dh.delete_flag, '0') <> '1'
            AND dh.sales_man ~ ('(^|,)' || p.id || '(,|$)')
      );

UPDATE jsh_person p
SET type = '销售员'
WHERE EXISTS (
      SELECT 1 FROM jsh_depot_head dh
      WHERE COALESCE(dh.delete_flag, '0') <> '1'
        AND dh.sales_man ~ ('(^|,)' || p.id || '(,|$)')
  );

UPDATE jsh_person p
SET type = '财务员'
WHERE EXISTS (
      SELECT 1 FROM jsh_account_head ah
      WHERE COALESCE(ah.delete_flag, '0') <> '1'
        AND ah.hands_person_id = p.id
  )
  AND NOT EXISTS (
      SELECT 1 FROM jsh_depot_head dh
      WHERE COALESCE(dh.delete_flag, '0') <> '1'
        AND dh.sales_man ~ ('(^|,)' || p.id || '(,|$)')
  );

DO $$
DECLARE
    source_person RECORD;
    finance_id BIGINT;
BEGIN
    FOR source_person IN
        SELECT DISTINCT p.*
        FROM jsh_person p
        JOIN jsh_account_head ah ON ah.hands_person_id = p.id
        WHERE COALESCE(ah.delete_flag, '0') <> '1'
          AND p.type <> '财务员'
    LOOP
        INSERT INTO jsh_person(type, name, enabled, sort, tenant_id, delete_flag)
        VALUES ('财务员', left(source_person.name, 25) || '-财务-' || source_person.id,
                source_person.enabled, source_person.sort, source_person.tenant_id, '0')
        RETURNING id INTO finance_id;

        UPDATE jsh_account_head
        SET hands_person_id = finance_id
        WHERE hands_person_id = source_person.id;
    END LOOP;
END $$;

UPDATE jsh_person
SET type = '销售员', delete_flag = '1'
WHERE type IS NULL OR type NOT IN ('销售员', '仓管员', '财务员');

WITH ranked AS (
    SELECT id,
           MIN(id) OVER (PARTITION BY tenant_id, name) AS keep_id,
           ROW_NUMBER() OVER (PARTITION BY tenant_id, name ORDER BY id) AS row_num
    FROM jsh_person
    WHERE delete_flag <> '1'
), duplicates AS (
    SELECT id, keep_id FROM ranked WHERE row_num > 1
)
UPDATE jsh_account_head ah
SET hands_person_id = duplicates.keep_id
FROM duplicates
WHERE ah.hands_person_id = duplicates.id;

WITH ranked AS (
    SELECT id,
           MIN(id) OVER (PARTITION BY tenant_id, name) AS keep_id,
           ROW_NUMBER() OVER (PARTITION BY tenant_id, name ORDER BY id) AS row_num
    FROM jsh_person
    WHERE delete_flag <> '1'
), duplicates AS (
    SELECT id, keep_id FROM ranked WHERE row_num > 1
), rewritten AS (
    SELECT dh.id,
           string_agg(DISTINCT COALESCE(d.keep_id, token.person_id)::text, ','
                      ORDER BY COALESCE(d.keep_id, token.person_id)::text) AS sales_man
    FROM jsh_depot_head dh
    CROSS JOIN LATERAL (
        SELECT value::bigint AS person_id
        FROM unnest(string_to_array(COALESCE(dh.sales_man, ''), ',')) value
        WHERE btrim(value) ~ '^[0-9]+$'
    ) token
    LEFT JOIN duplicates d ON d.id = token.person_id
    GROUP BY dh.id
)
UPDATE jsh_depot_head dh
SET sales_man = rewritten.sales_man
FROM rewritten
WHERE dh.id = rewritten.id;

WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY tenant_id, name ORDER BY id) AS row_num
    FROM jsh_person
    WHERE delete_flag <> '1'
)
UPDATE jsh_person p
SET delete_flag = '1'
FROM ranked
WHERE p.id = ranked.id AND ranked.row_num > 1;

ALTER TABLE jsh_person ALTER COLUMN name SET NOT NULL;
ALTER TABLE jsh_person ALTER COLUMN type SET NOT NULL;
ALTER TABLE jsh_person ALTER COLUMN enabled SET DEFAULT TRUE;
ALTER TABLE jsh_person ALTER COLUMN enabled SET NOT NULL;
ALTER TABLE jsh_person ALTER COLUMN tenant_id SET DEFAULT 0;
ALTER TABLE jsh_person ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE jsh_person ALTER COLUMN delete_flag SET DEFAULT '0';
ALTER TABLE jsh_person ALTER COLUMN delete_flag SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'chk_person_type' AND conrelid = 'jsh_person'::regclass) THEN
        ALTER TABLE jsh_person ADD CONSTRAINT chk_person_type
            CHECK (type IN ('销售员', '仓管员', '财务员'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'chk_person_sort' AND conrelid = 'jsh_person'::regclass) THEN
        ALTER TABLE jsh_person ADD CONSTRAINT chk_person_sort
            CHECK (sort IS NULL OR sort ~ '^[0-9]{1,10}$');
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_person_active_name
    ON jsh_person(tenant_id, name)
    WHERE delete_flag <> '1';

COMMIT;
