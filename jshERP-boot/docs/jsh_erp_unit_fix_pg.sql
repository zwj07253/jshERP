-- Run this migration before deploying the multi-unit validation changes.
-- Duplicate active names must be resolved before creating the index.
CREATE UNIQUE INDEX IF NOT EXISTS uk_unit_tenant_name_active
    ON jsh_unit(COALESCE(tenant_id, 0), name)
    WHERE COALESCE(delete_flag, '0') != '1';

CREATE UNIQUE INDEX IF NOT EXISTS uk_material_extend_tenant_material_sku_active
    ON jsh_material_extend(COALESCE(tenant_id, 0), material_id, sku)
    WHERE COALESCE(delete_flag, '0') != '1' AND COALESCE(sku, '') != '';

-- Duplicate active supplier/customer/member names must be resolved first.
CREATE UNIQUE INDEX IF NOT EXISTS uk_supplier_tenant_type_name_active
    ON jsh_supplier(COALESCE(tenant_id, 0), type, supplier)
    WHERE COALESCE(delete_flag, '0') != '1';

-- 应用约束前请先处理同一租户内的重名仓库及多个默认仓库。
CREATE UNIQUE INDEX IF NOT EXISTS uk_depot_tenant_name_active
ON jsh_depot(COALESCE(tenant_id, 0), name)
WHERE COALESCE(delete_flag, '0') != '1';

CREATE UNIQUE INDEX IF NOT EXISTS uk_depot_tenant_default_active
ON jsh_depot(COALESCE(tenant_id, 0))
WHERE COALESCE(delete_flag, '0') != '1' AND is_default IS TRUE;
