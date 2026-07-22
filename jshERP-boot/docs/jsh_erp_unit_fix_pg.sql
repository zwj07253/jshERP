-- Run this migration before deploying the multi-unit validation changes.
-- Duplicate active names must be resolved before creating the index.
CREATE UNIQUE INDEX IF NOT EXISTS uk_unit_tenant_name_active
    ON jsh_unit(COALESCE(tenant_id, 0), name)
    WHERE COALESCE(delete_flag, '0') != '1';
