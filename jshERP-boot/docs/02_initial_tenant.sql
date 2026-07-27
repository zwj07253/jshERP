-- 生产环境初始租户引导脚本（PostgreSQL）。
--
-- 仅在全新的生产数据库上、执行完 jsh_erp_pg.sql 之后运行。
-- 创建一个客户租户及其管理员；不创建示例商品、客户、仓库、库存或单据。
--
-- 用法：
--   psql -U postgres -d jsh_erp -f jshERP-boot/docs/02_initial_tenant.sql
--
-- 部署前请按需修改以下值。密码为 123456 的 bcrypt 哈希，首次登录后请立即修改。
--
-- 权限模型说明：
--   平台管理员（admin）  → 租户管理、平台配置、菜单管理、插件管理、字典管理、平台级角色
--   租户管理员            → 商品、仓库、客户/供应商、单据、账户、用户、部门、角色、系统配置、日志
--   租户管理员不可见      → 平台配置、租户管理、菜单管理、插件管理、字典管理
--
--   角色 ID 10 是"租户管理员模板"，tenant_id 为空表示全局模板。
--   admin 新建租户时，系统会复制此模板为新租户创建专属角色。

BEGIN;

-- ========================================
-- 租户表 jsh_tenant
-- ========================================
INSERT INTO jsh_tenant
    (id, tenant_id, login_name, user_num_limit, type, enabled, create_time, expire_time, remark, delete_flag)
VALUES
    (1, 1, 'jsh', 50, '0', TRUE, NOW(), NULL, '初始生产租户', '0')
ON CONFLICT (id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    login_name = EXCLUDED.login_name,
    user_num_limit = EXCLUDED.user_num_limit,
    type = EXCLUDED.type,
    enabled = EXCLUDED.enabled,
    expire_time = EXCLUDED.expire_time,
    remark = EXCLUDED.remark,
    delete_flag = EXCLUDED.delete_flag;

-- ========================================
-- 角色表 jsh_role
-- ========================================
-- 角色 1：租户 1 的管理员角色（tenant_id=1，仅含租户业务功能）
INSERT INTO jsh_role
    (id, name, type, price_limit, value, description, enabled, sort, tenant_id, delete_flag)
VALUES
    (1, '租户管理员', 'role', NULL, '1', '初始租户管理员，拥有全部租户业务菜单权限', TRUE, '1', 1, '0')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    type = EXCLUDED.type,
    description = EXCLUDED.description,
    enabled = EXCLUDED.enabled,
    tenant_id = EXCLUDED.tenant_id,
    delete_flag = EXCLUDED.delete_flag;

-- 角色 4：平台管理员（admin 专用，含全部功能含平台专属）
INSERT INTO jsh_role
    (id, name, type, price_limit, value, description, enabled, sort, tenant_id, delete_flag)
VALUES
    (4, '平台管理员', '全部数据', NULL, '1', '平台超级管理员，拥有全部功能权限', TRUE, '0', NULL, '0')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    type = EXCLUDED.type,
    description = EXCLUDED.description,
    enabled = EXCLUDED.enabled,
    delete_flag = EXCLUDED.delete_flag;

-- 角色 10：租户管理员模板（全局模板，tenant_id=NULL，admin 新建租户时复制此模板）
-- 包含全部租户业务功能，不包含平台专属功能（菜单管理16、租户管理18、插件管理245、平台配置258、字典管理260）
INSERT INTO jsh_role
    (id, name, type, price_limit, value, description, enabled, sort, tenant_id, delete_flag)
VALUES
    (10, '租户管理员模板', 'role', NULL, '1', '租户管理员角色模板，新建租户时自动复制', TRUE, '0', NULL, '0')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    type = EXCLUDED.type,
    description = EXCLUDED.description,
    enabled = EXCLUDED.enabled,
    delete_flag = EXCLUDED.delete_flag;

-- ========================================
-- 用户表 jsh_user
-- ========================================
INSERT INTO jsh_user
    (id, username, login_name, password, leader_flag, position, department, email, phonenum,
     ismanager, isystem, status, description, remark, weixin_open_id, tenant_id, delete_flag)
VALUES
    (1, '租户管理员', 'jsh', '$2a$12$GvqZrBIMrJGccj9nFN.yKu80vFEjlnZfsiJkC8TOqhBEpPub3LIXq', '1', NULL, NULL, NULL, NULL,
     0, 0, 0, '初始生产租户管理员', NULL, NULL, 1, '0')
ON CONFLICT (id) DO UPDATE SET
    username = EXCLUDED.username,
    login_name = EXCLUDED.login_name,
    password = EXCLUDED.password,
    leader_flag = EXCLUDED.leader_flag,
    position = EXCLUDED.position,
    department = EXCLUDED.department,
    email = EXCLUDED.email,
    phonenum = EXCLUDED.phonenum,
    ismanager = EXCLUDED.ismanager,
    isystem = EXCLUDED.isystem,
    status = EXCLUDED.status,
    description = EXCLUDED.description,
    remark = EXCLUDED.remark,
    weixin_open_id = EXCLUDED.weixin_open_id,
    tenant_id = EXCLUDED.tenant_id,
    delete_flag = EXCLUDED.delete_flag;

-- ========================================
-- 用户业务关联表 jsh_user_business
-- ========================================

-- 用户-角色关联：将用户 1 绑定到角色 1
INSERT INTO jsh_user_business (type, key_id, value, btn_str, tenant_id, delete_flag)
VALUES
    ('UserRole', '1', '[1]', NULL, 1, '0')
ON CONFLICT (COALESCE(tenant_id, 0), type, key_id) WHERE COALESCE(delete_flag, '0') != '1'
DO UPDATE SET value = EXCLUDED.value, btn_str = EXCLUDED.btn_str, delete_flag = EXCLUDED.delete_flag;

-- 平台管理员（admin）角色-菜单权限：全部功能含平台专属
INSERT INTO jsh_user_business (type, key_id, value, btn_str, tenant_id, delete_flag)
VALUES
    ('RoleFunctions', '4',
     '[1][13][14][15][16][18][21][22][23][24][25][26][31][32][33][38][40][41][44][59][194][195][197][198][199][200][201][202][203][204][205][206][207][208][209][210][211][212][217][218][220][225][226][227][228][229][232][233][234][235][236][237][239][241][242][243][244][245][246][247][248][258][259][260][261]',
     '[{"funId":13,"btnStr":"1"},{"funId":14,"btnStr":"1"},{"funId":243,"btnStr":"1"},{"funId":234,"btnStr":"1"},{"funId":236,"btnStr":"1"},{"funId":16,"btnStr":"1"},{"funId":18,"btnStr":"1"},{"funId":258,"btnStr":"1"},{"funId":22,"btnStr":"1"},{"funId":23,"btnStr":"1,3"},{"funId":220,"btnStr":"1"},{"funId":247,"btnStr":"1"},{"funId":25,"btnStr":"1,3"},{"funId":217,"btnStr":"1,3"},{"funId":218,"btnStr":"1"},{"funId":26,"btnStr":"1"},{"funId":194,"btnStr":"1"},{"funId":195,"btnStr":"1"},{"funId":31,"btnStr":"1"},{"funId":261,"btnStr":"1,2,3,7"},{"funId":241,"btnStr":"1,2,3,7"},{"funId":33,"btnStr":"1,2,3,7"},{"funId":199,"btnStr":"1,2,3,7"},{"funId":242,"btnStr":"1,2,3,7"},{"funId":41,"btnStr":"1,2,3,7"},{"funId":200,"btnStr":"1,2,3,7"},{"funId":210,"btnStr":"1,2,3,7"},{"funId":211,"btnStr":"1,2,3,7"},{"funId":197,"btnStr":"1,2,3,7"},{"funId":203,"btnStr":"1,2,3,7"},{"funId":204,"btnStr":"1,2,3,7"},{"funId":205,"btnStr":"1,2,3,7"},{"funId":206,"btnStr":"1,2,3,7"},{"funId":212,"btnStr":"1,2,3,7"},{"funId":201,"btnStr":"1,2,3,7"},{"funId":202,"btnStr":"1,2,3,7"},{"funId":40,"btnStr":"1,2,3,7"},{"funId":232,"btnStr":"1,2,3,7"},{"funId":233,"btnStr":"1,2,3,7"}]',
     NULL, '0')
ON CONFLICT (COALESCE(tenant_id, 0), type, key_id) WHERE COALESCE(delete_flag, '0') != '1'
DO UPDATE SET value = EXCLUDED.value, btn_str = EXCLUDED.btn_str, delete_flag = EXCLUDED.delete_flag;

-- 租户 1 管理员角色-菜单权限：仅租户业务功能（不含平台专属：16, 18, 245, 258, 260）
INSERT INTO jsh_user_business (type, key_id, value, btn_str, tenant_id, delete_flag)
VALUES
    ('RoleFunctions', '1',
     '[1][13][14][15][21][22][23][24][25][26][31][32][33][38][40][41][44][59][194][195][197][198][199][200][201][202][203][204][205][206][207][208][209][210][211][212][217][218][220][225][226][227][228][229][232][233][234][235][236][237][239][241][242][243][244][246][247][248][259][261]',
     '[{"funId":13,"btnStr":"1"},{"funId":14,"btnStr":"1"},{"funId":243,"btnStr":"1"},{"funId":234,"btnStr":"1"},{"funId":236,"btnStr":"1"},{"funId":22,"btnStr":"1"},{"funId":23,"btnStr":"1,3"},{"funId":220,"btnStr":"1"},{"funId":247,"btnStr":"1"},{"funId":25,"btnStr":"1,3"},{"funId":217,"btnStr":"1,3"},{"funId":218,"btnStr":"1"},{"funId":26,"btnStr":"1"},{"funId":194,"btnStr":"1"},{"funId":195,"btnStr":"1"},{"funId":31,"btnStr":"1"},{"funId":261,"btnStr":"1,2,3,7"},{"funId":241,"btnStr":"1,2,3,7"},{"funId":33,"btnStr":"1,2,3,7"},{"funId":199,"btnStr":"1,2,3,7"},{"funId":242,"btnStr":"1,2,3,7"},{"funId":41,"btnStr":"1,2,3,7"},{"funId":200,"btnStr":"1,2,3,7"},{"funId":210,"btnStr":"1,2,3,7"},{"funId":211,"btnStr":"1,2,3,7"},{"funId":197,"btnStr":"1,2,3,7"},{"funId":203,"btnStr":"1,2,3,7"},{"funId":204,"btnStr":"1,2,3,7"},{"funId":205,"btnStr":"1,2,3,7"},{"funId":206,"btnStr":"1,2,3,7"},{"funId":212,"btnStr":"1,2,3,7"},{"funId":201,"btnStr":"1,2,3,7"},{"funId":202,"btnStr":"1,2,3,7"},{"funId":40,"btnStr":"1,2,3,7"},{"funId":232,"btnStr":"1,2,3,7"},{"funId":233,"btnStr":"1,2,3,7"}]',
     1, '0')
ON CONFLICT (COALESCE(tenant_id, 0), type, key_id) WHERE COALESCE(delete_flag, '0') != '1'
DO UPDATE SET value = EXCLUDED.value, btn_str = EXCLUDED.btn_str, delete_flag = EXCLUDED.delete_flag;

-- 租户管理员模板（角色 10）- 菜单权限：与角色 1 相同的租户业务功能（全局模板，tenant_id=NULL）
INSERT INTO jsh_user_business (type, key_id, value, btn_str, tenant_id, delete_flag)
VALUES
    ('RoleFunctions', '10',
     '[1][13][14][15][21][22][23][24][25][26][31][32][33][38][40][41][44][59][194][195][197][198][199][200][201][202][203][204][205][206][207][208][209][210][211][212][217][218][220][225][226][227][228][229][232][233][234][235][236][237][239][241][242][243][244][246][247][248][259][261]',
     '[{"funId":13,"btnStr":"1"},{"funId":14,"btnStr":"1"},{"funId":243,"btnStr":"1"},{"funId":234,"btnStr":"1"},{"funId":236,"btnStr":"1"},{"funId":22,"btnStr":"1"},{"funId":23,"btnStr":"1,3"},{"funId":220,"btnStr":"1"},{"funId":247,"btnStr":"1"},{"funId":25,"btnStr":"1,3"},{"funId":217,"btnStr":"1,3"},{"funId":218,"btnStr":"1"},{"funId":26,"btnStr":"1"},{"funId":194,"btnStr":"1"},{"funId":195,"btnStr":"1"},{"funId":31,"btnStr":"1"},{"funId":261,"btnStr":"1,2,3,7"},{"funId":241,"btnStr":"1,2,3,7"},{"funId":33,"btnStr":"1,2,3,7"},{"funId":199,"btnStr":"1,2,3,7"},{"funId":242,"btnStr":"1,2,3,7"},{"funId":41,"btnStr":"1,2,3,7"},{"funId":200,"btnStr":"1,2,3,7"},{"funId":210,"btnStr":"1,2,3,7"},{"funId":211,"btnStr":"1,2,3,7"},{"funId":197,"btnStr":"1,2,3,7"},{"funId":203,"btnStr":"1,2,3,7"},{"funId":204,"btnStr":"1,2,3,7"},{"funId":205,"btnStr":"1,2,3,7"},{"funId":206,"btnStr":"1,2,3,7"},{"funId":212,"btnStr":"1,2,3,7"},{"funId":201,"btnStr":"1,2,3,7"},{"funId":202,"btnStr":"1,2,3,7"},{"funId":40,"btnStr":"1,2,3,7"},{"funId":232,"btnStr":"1,2,3,7"},{"funId":233,"btnStr":"1,2,3,7"}]',
     NULL, '0')
ON CONFLICT (COALESCE(tenant_id, 0), type, key_id) WHERE COALESCE(delete_flag, '0') != '1'
DO UPDATE SET value = EXCLUDED.value, btn_str = EXCLUDED.btn_str, delete_flag = EXCLUDED.delete_flag;

-- ========================================
-- 租户系统配置表 jsh_system_config
-- ========================================
INSERT INTO jsh_system_config
    (company_name, depot_flag, customer_flag, minus_stock_flag, purchase_by_sale_flag,
     multi_level_approval_flag, force_approval_flag, update_unit_price_flag,
     over_link_bill_flag, in_out_manage_flag, multi_account_flag, move_avg_price_flag,
     audit_print_flag, zero_change_amount_flag, customer_static_price_flag,
     material_price_tax_flag, tenant_id, delete_flag)
VALUES
    ('请配置公司信息', '0', '0', '0', '0', '0', '0', '1',
     '0', '0', '0', '0', '0', '0', '0', '0', 1, '0')
ON CONFLICT (tenant_id) DO UPDATE SET
    company_name = EXCLUDED.company_name,
    delete_flag = EXCLUDED.delete_flag;

-- ========================================
-- 序列同步（确保后续 INSERT 不会主键冲突）
-- ========================================
SELECT setval('jsh_tenant_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM jsh_tenant), 1));
SELECT setval('jsh_role_id_seq', GREATEST((SELECT COALESCE(MAX(id), 10) FROM jsh_role), 10));
SELECT setval('jsh_user_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM jsh_user), 1));
SELECT setval('jsh_user_business_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM jsh_user_business), 1));
SELECT setval('jsh_system_config_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM jsh_system_config), 1));

COMMIT;
