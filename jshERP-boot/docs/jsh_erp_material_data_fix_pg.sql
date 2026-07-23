BEGIN;

-- 清理错误类别引用和孤立 SKU。SKU 采用软删除以保留审计信息。
UPDATE jsh_material m
SET category_id = NULL
WHERE m.category_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM jsh_material_category c
      WHERE c.id = m.category_id
        AND c.tenant_id IS NOT DISTINCT FROM m.tenant_id
        AND COALESCE(c.delete_flag, '0') != '1'
  );

UPDATE jsh_material_extend me
SET delete_flag = '1'
WHERE COALESCE(me.delete_flag, '0') != '1'
  AND NOT EXISTS (
      SELECT 1 FROM jsh_material m
      WHERE m.id = me.material_id
        AND m.tenant_id IS NOT DISTINCT FROM me.tenant_id
        AND COALESCE(m.delete_flag, '0') != '1'
  );

-- 合并重复属性名称，并同步商品 JSON 中保存的属性 id。
CREATE TEMP TABLE tmp_material_attribute_duplicate ON COMMIT DROP AS
SELECT id,
       min(id) OVER (PARTITION BY COALESCE(tenant_id, 0), lower(btrim(attribute_name))) AS keep_id
FROM jsh_material_attribute
WHERE COALESCE(delete_flag, '0') != '1';

WITH rewritten AS (
    SELECT m.id,
           jsonb_set(
               m.attribute::jsonb,
               '{manySku}',
               COALESCE(jsonb_agg(to_jsonb(COALESCE(d.keep_id::text, item.value))), '[]'::jsonb)
           ) AS attribute_json
    FROM jsh_material m
    CROSS JOIN LATERAL jsonb_array_elements_text(COALESCE(m.attribute::jsonb -> 'manySku', '[]'::jsonb)) item(value)
    LEFT JOIN tmp_material_attribute_duplicate d ON d.id::text = item.value
    WHERE m.attribute IS NOT NULL AND btrim(m.attribute) != ''
    GROUP BY m.id, m.attribute
)
UPDATE jsh_material m
SET attribute = rewritten.attribute_json::text
FROM rewritten
WHERE m.id = rewritten.id;

UPDATE jsh_material_attribute a
SET delete_flag = '1'
FROM tmp_material_attribute_duplicate d
WHERE a.id = d.id AND d.id <> d.keep_id;

-- 同一租户每个扩展字段只保留一条有效配置，采用最后一次配置的别名。
WITH duplicate AS (
    SELECT id,
           min(id) OVER (PARTITION BY COALESCE(tenant_id, 0), native_name) AS keep_id,
           first_value(another_name) OVER (
               PARTITION BY COALESCE(tenant_id, 0), native_name ORDER BY id DESC
           ) AS latest_alias
    FROM jsh_material_property
    WHERE COALESCE(delete_flag, '0') != '1'
), update_keeper AS (
    UPDATE jsh_material_property p
    SET another_name = d.latest_alias
    FROM duplicate d
    WHERE p.id = d.keep_id
    RETURNING p.id
)
UPDATE jsh_material_property p
SET delete_flag = '1'
FROM duplicate d
WHERE p.id = d.id AND d.id <> d.keep_id;

-- 每个商品只保留一个默认 SKU。
WITH ranked AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY COALESCE(tenant_id, 0), material_id
               ORDER BY CASE WHEN default_flag = '1' THEN 0 ELSE 1 END, id
           ) AS rn
    FROM jsh_material_extend
    WHERE COALESCE(delete_flag, '0') != '1'
)
UPDATE jsh_material_extend me
SET default_flag = CASE WHEN ranked.rn = 1 THEN '1' ELSE '0' END
FROM ranked
WHERE me.id = ranked.id;

CREATE UNIQUE INDEX IF NOT EXISTS uk_material_attribute_tenant_name_active
    ON jsh_material_attribute(COALESCE(tenant_id, 0), lower(btrim(attribute_name)))
    WHERE COALESCE(delete_flag, '0') != '1';

CREATE UNIQUE INDEX IF NOT EXISTS uk_material_property_tenant_native_active
    ON jsh_material_property(COALESCE(tenant_id, 0), native_name)
    WHERE COALESCE(delete_flag, '0') != '1';

DROP INDEX IF EXISTS uk_material_extend_tenant_default_active;

CREATE OR REPLACE FUNCTION jsh_validate_material_category_reference()
RETURNS trigger AS $$
BEGIN
    IF NEW.category_id IS NOT NULL AND NEW.category_id <= 0 THEN
        NEW.category_id := NULL;
    END IF;
    IF NEW.category_id IS NOT NULL AND COALESCE(NEW.delete_flag, '0') != '1'
       AND NOT EXISTS (
           SELECT 1 FROM jsh_material_category c
           WHERE c.id = NEW.category_id
             AND c.tenant_id IS NOT DISTINCT FROM NEW.tenant_id
             AND COALESCE(c.delete_flag, '0') != '1'
       ) THEN
        RAISE EXCEPTION 'material category does not exist or is deleted';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_material_category_reference ON jsh_material;
CREATE TRIGGER trg_material_category_reference
BEFORE INSERT OR UPDATE OF category_id, tenant_id, delete_flag ON jsh_material
FOR EACH ROW EXECUTE FUNCTION jsh_validate_material_category_reference();

CREATE OR REPLACE FUNCTION jsh_validate_material_extend_reference()
RETURNS trigger AS $$
BEGIN
    IF COALESCE(NEW.delete_flag, '0') != '1'
       AND NOT EXISTS (
           SELECT 1 FROM jsh_material m
           WHERE m.id = NEW.material_id
             AND m.tenant_id IS NOT DISTINCT FROM NEW.tenant_id
             AND COALESCE(m.delete_flag, '0') != '1'
       ) THEN
        RAISE EXCEPTION 'material extend references a missing or deleted material';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_material_extend_reference ON jsh_material_extend;
CREATE TRIGGER trg_material_extend_reference
BEFORE INSERT OR UPDATE OF material_id, tenant_id, delete_flag ON jsh_material_extend
FOR EACH ROW EXECUTE FUNCTION jsh_validate_material_extend_reference();

CREATE OR REPLACE FUNCTION jsh_validate_single_default_material_extend()
RETURNS trigger AS $$
BEGIN
    IF (SELECT count(*) FROM jsh_material_extend me
        WHERE me.material_id = NEW.material_id
          AND me.tenant_id IS NOT DISTINCT FROM NEW.tenant_id
          AND COALESCE(me.delete_flag, '0') != '1'
          AND me.default_flag = '1') > 1 THEN
        RAISE EXCEPTION 'a material can only have one default extend row';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_material_extend_single_default ON jsh_material_extend;
CREATE CONSTRAINT TRIGGER trg_material_extend_single_default
AFTER INSERT OR UPDATE OF material_id, tenant_id, delete_flag, default_flag ON jsh_material_extend
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION jsh_validate_single_default_material_extend();

COMMIT;
