BEGIN;

LOCK TABLE jsh_organization IN SHARE ROW EXCLUSIVE MODE;
LOCK TABLE jsh_orga_user_rel IN SHARE ROW EXCLUSIVE MODE;
LOCK TABLE jsh_user IN SHARE ROW EXCLUSIVE MODE;

UPDATE jsh_organization o
SET tenant_id = source.tenant_id
FROM (
    SELECT orga_id, MIN(tenant_id) AS tenant_id
    FROM jsh_orga_user_rel
    WHERE orga_id IS NOT NULL AND tenant_id IS NOT NULL
    GROUP BY orga_id
) source
WHERE o.id = source.orga_id AND o.tenant_id IS NULL;

UPDATE jsh_organization
SET org_abr = CASE WHEN btrim(COALESCE(org_abr, '')) = '' THEN id::text
                   ELSE left(btrim(org_abr), 20) END,
    org_no = CASE WHEN btrim(COALESCE(org_no, '')) = '' THEN id::text
                  ELSE left(btrim(org_no), 20) END,
    sort = CASE WHEN btrim(COALESCE(sort, '')) ~ '^[0-9]{1,10}$'
                THEN (btrim(sort)::bigint)::text ELSE NULL END,
    remark = left(remark, 500),
    tenant_id = COALESCE(tenant_id, 0),
    delete_flag = COALESCE(delete_flag, '0');

UPDATE jsh_orga_user_rel rel
SET tenant_id = COALESCE(u.tenant_id, rel.tenant_id, 0),
    delete_flag = COALESCE(rel.delete_flag, '0')
FROM jsh_user u
WHERE u.id = rel.user_id;

UPDATE jsh_organization o
SET delete_flag = '0'
WHERE EXISTS (
    SELECT 1 FROM jsh_orga_user_rel rel
    WHERE rel.orga_id = o.id AND COALESCE(rel.delete_flag, '0') <> '1'
      AND COALESCE(rel.tenant_id, 0) = COALESCE(o.tenant_id, 0)
);

UPDATE jsh_organization child
SET parent_id = NULL
WHERE parent_id = child.id
   OR NOT EXISTS (
       SELECT 1 FROM jsh_organization parent
       WHERE parent.id = child.parent_id
         AND parent.delete_flag <> '1'
         AND parent.tenant_id = child.tenant_id
   );

DO $$
DECLARE
    start_org RECORD;
    cursor_id BIGINT;
    next_id BIGINT;
    visited BIGINT[];
BEGIN
    FOR start_org IN SELECT id, parent_id FROM jsh_organization WHERE parent_id IS NOT NULL LOOP
        cursor_id := start_org.id;
        next_id := start_org.parent_id;
        visited := ARRAY[start_org.id];
        WHILE next_id IS NOT NULL LOOP
            IF next_id = ANY(visited) THEN
                UPDATE jsh_organization SET parent_id = NULL WHERE id = start_org.id;
                EXIT;
            END IF;
            visited := array_append(visited, next_id);
            SELECT parent_id INTO next_id FROM jsh_organization WHERE id = next_id;
            IF NOT FOUND THEN
                EXIT;
            END IF;
        END LOOP;
    END LOOP;
END $$;

WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY tenant_id, org_abr ORDER BY id) AS row_num
    FROM jsh_organization WHERE delete_flag <> '1'
)
UPDATE jsh_organization o
SET org_abr = o.id::text
FROM ranked r
WHERE o.id = r.id AND r.row_num > 1;

WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY tenant_id, org_no ORDER BY id) AS row_num
    FROM jsh_organization WHERE delete_flag <> '1'
)
UPDATE jsh_organization o
SET org_no = o.id::text
FROM ranked r
WHERE o.id = r.id AND r.row_num > 1;

DELETE FROM jsh_orga_user_rel rel
WHERE NOT EXISTS (SELECT 1 FROM jsh_user u WHERE u.id = rel.user_id);

UPDATE jsh_orga_user_rel rel
SET delete_flag = '1', orga_id = NULL, update_time = CURRENT_TIMESTAMP
WHERE rel.orga_id IS NOT NULL AND NOT EXISTS (
       SELECT 1 FROM jsh_organization o
       WHERE o.id = rel.orga_id AND o.delete_flag <> '1' AND o.tenant_id = rel.tenant_id
   );

WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (
        PARTITION BY tenant_id, user_id
        ORDER BY CASE WHEN orga_id IS NULL THEN 1 ELSE 0 END, id
    ) AS row_num
    FROM jsh_orga_user_rel WHERE delete_flag <> '1'
)
UPDATE jsh_orga_user_rel rel
SET delete_flag = '1', update_time = CURRENT_TIMESTAMP
FROM ranked r
WHERE rel.id = r.id AND r.row_num > 1;

UPDATE jsh_orga_user_rel SET tenant_id = 0 WHERE tenant_id IS NULL;

ALTER TABLE jsh_organization ALTER COLUMN org_no SET NOT NULL;
ALTER TABLE jsh_organization ALTER COLUMN org_abr SET NOT NULL;
ALTER TABLE jsh_organization ALTER COLUMN tenant_id SET DEFAULT 0;
ALTER TABLE jsh_organization ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE jsh_organization ALTER COLUMN delete_flag SET DEFAULT '0';
ALTER TABLE jsh_organization ALTER COLUMN delete_flag SET NOT NULL;
ALTER TABLE jsh_orga_user_rel ALTER COLUMN tenant_id SET DEFAULT 0;
ALTER TABLE jsh_orga_user_rel ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE jsh_orga_user_rel ALTER COLUMN delete_flag SET DEFAULT '0';
ALTER TABLE jsh_orga_user_rel ALTER COLUMN delete_flag SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_organization_parent') THEN
        ALTER TABLE jsh_organization ADD CONSTRAINT chk_organization_parent
            CHECK (parent_id IS NULL OR parent_id <> id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_organization_sort') THEN
        ALTER TABLE jsh_organization ADD CONSTRAINT chk_organization_sort
            CHECK (sort IS NULL OR sort ~ '^[0-9]{1,10}$');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_organization_parent') THEN
        ALTER TABLE jsh_organization ADD CONSTRAINT fk_organization_parent
            FOREIGN KEY (parent_id) REFERENCES jsh_organization(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_orga_user_rel_organization') THEN
        ALTER TABLE jsh_orga_user_rel ADD CONSTRAINT fk_orga_user_rel_organization
            FOREIGN KEY (orga_id) REFERENCES jsh_organization(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_orga_user_rel_user') THEN
        ALTER TABLE jsh_orga_user_rel ADD CONSTRAINT fk_orga_user_rel_user
            FOREIGN KEY (user_id) REFERENCES jsh_user(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_organization_parent_id ON jsh_organization(parent_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_organization_active_name
    ON jsh_organization(tenant_id, org_abr) WHERE delete_flag <> '1';
CREATE UNIQUE INDEX IF NOT EXISTS uk_organization_active_no
    ON jsh_organization(tenant_id, org_no) WHERE delete_flag <> '1';
CREATE UNIQUE INDEX IF NOT EXISTS uk_orga_user_rel_active_user
    ON jsh_orga_user_rel(tenant_id, user_id) WHERE delete_flag <> '1';

COMMIT;
