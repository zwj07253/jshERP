-- Apply this migration before deploying the material information integrity fixes.
-- Resolve any duplicate active bar codes reported by PostgreSQL before retrying.
CREATE UNIQUE INDEX IF NOT EXISTS uk_material_extend_tenant_bar_code_active
    ON jsh_material_extend(COALESCE(tenant_id, 0), bar_code)
    WHERE COALESCE(delete_flag, '0') != '1' AND bar_code IS NOT NULL AND bar_code != '';
