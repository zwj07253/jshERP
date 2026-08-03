-- Convert an existing PostgreSQL deployment to the supported single-tenant mode.
--
-- Before running, confirm that v_tenant_id is the customer's business tenant.
-- The script is transactional and can safely be re-run.

BEGIN;

DO $$
DECLARE
    v_tenant_id bigint := 1;
    v_permission record;
BEGIN
    -- Fail before changing data if a targeted button-permission payload cannot
    -- be read as a JSON array. This avoids silently discarding legacy data.
    FOR v_permission IN
        SELECT ub.id, ub.btn_str
        FROM jsh_user_business ub
        WHERE ub.type = 'RoleFunctions'
          AND ub.key_id IN (
              SELECT id::text FROM jsh_role WHERE tenant_id = v_tenant_id
          )
          AND ub.btn_str IS NOT NULL
          AND btrim(ub.btn_str) <> ''
    LOOP
        BEGIN
            IF jsonb_typeof(v_permission.btn_str::jsonb) <> 'array' THEN
                RAISE EXCEPTION 'RoleFunctions btn_str must be a JSON array (id=%)', v_permission.id;
            END IF;
        EXCEPTION
            WHEN invalid_text_representation THEN
                RAISE EXCEPTION 'RoleFunctions btn_str is invalid JSON (id=%)', v_permission.id;
        END;
    END LOOP;

    -- Tenant records are retained as data boundaries, but no longer expire or
    -- impose a practical user quota in the single-tenant deployment.
    UPDATE jsh_tenant
    SET expire_time = NULL,
        user_num_limit = 999999999,
        enabled = true
    WHERE tenant_id = v_tenant_id;

    -- Keep the backend registration guard enabled, but turn public registration off.
    UPDATE jsh_platform_config
    SET platform_value = '0'
    WHERE platform_key = 'register_flag';

    -- Remove platform-only menus from all roles belonging to the business tenant.
    UPDATE jsh_user_business
    SET value = replace(replace(replace(replace(replace(value,
                    '[16]', ''), '[18]', ''), '[245]', ''), '[258]', ''), '[260]', '')
    WHERE type = 'RoleFunctions'
      AND key_id IN (
          SELECT id::text FROM jsh_role WHERE tenant_id = v_tenant_id
      )
      AND value ~ '\\[(16|18|245|258|260)\\]';

    UPDATE jsh_user_business ub
    SET btn_str = (
        SELECT jsonb_agg(item.value ORDER BY item.ordinality)::text
        FROM jsonb_array_elements(ub.btn_str::jsonb) WITH ORDINALITY AS item(value, ordinality)
        WHERE item.value->>'funId' NOT IN ('16', '18', '245', '258', '260')
    )
    WHERE ub.type = 'RoleFunctions'
      AND ub.key_id IN (
          SELECT id::text FROM jsh_role WHERE tenant_id = v_tenant_id
      )
      AND ub.btn_str IS NOT NULL
      AND btrim(ub.btn_str) <> ''
      AND EXISTS (
          SELECT 1
          FROM jsonb_array_elements(ub.btn_str::jsonb) AS item(value)
          WHERE item.value->>'funId' IN ('16', '18', '245', '258', '260')
      );

    RAISE NOTICE 'Single-tenant migration completed for tenant %', v_tenant_id;
END $$;

COMMIT;
