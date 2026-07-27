-- 生产环境初始租户引导脚本（PostgreSQL）。
--
-- 仅在全新的生产数据库上、执行完 jsh_erp_pg.sql 之后运行。
-- 创建一个客户租户及其管理员；不创建示例商品、客户、仓库、库存或单据。
--
-- 用法：
--   psql -U postgres -d jsh_erp -f jshERP-boot/docs/02_initial_tenant.sql
--
-- 部署前请按需修改以下值。密码为 123456 的 bcrypt 哈希，首次登录后请立即修改。

BEGIN;

-- ========================================
-- 租户表 jsh_tenant
-- ========================================
-- id              : 主键
-- tenant_id       : 租户编号（与 id 保持一致）
-- login_name      : 租户登录名
-- user_num_limit  : 用户数上限
-- type            : 租户类型（0=普通租户，1=试用租户）
-- enabled         : 是否启用
-- create_time     : 创建时间
-- expire_time     : 过期时间（NULL 表示永不过期）
-- remark          : 备注
-- delete_flag     : 删除标记（0=正常，1=已删除）
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
-- id              : 主键
-- name            : 角色名称
-- type            : 角色类型（role=普通角色）
-- price_limit     : 价格权限上限（NULL 表示不限制）
-- value           : 角色值
-- description     : 角色描述
-- enabled         : 是否启用
-- sort            : 排序号
-- tenant_id       : 所属租户编号
-- delete_flag     : 删除标记
INSERT INTO jsh_role
    (id, name, type, price_limit, value, description, enabled, sort, tenant_id, delete_flag)
VALUES
    (1, '租户管理员', 'role', NULL, '1', '初始租户管理员，拥有全部已启用菜单权限', TRUE, '1', 1, '0')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    type = EXCLUDED.type,
    price_limit = EXCLUDED.price_limit,
    value = EXCLUDED.value,
    description = EXCLUDED.description,
    enabled = EXCLUDED.enabled,
    sort = EXCLUDED.sort,
    tenant_id = EXCLUDED.tenant_id,
    delete_flag = EXCLUDED.delete_flag;

-- ========================================
-- 用户表 jsh_user
-- ========================================
-- id              : 主键
-- username        : 用户显示名称
-- login_name      : 登录账号
-- password        : 密码（bcrypt 哈希，明文为 123456）
-- leader_flag     : 是否负责人（1=是，0=否）
-- position        : 职位
-- department      : 部门
-- email           : 邮箱
-- phonenum        : 手机号
-- ismanager       : 是否管理员（0=否）
-- isystem         : 是否系统内置（0=否）
-- status          : 状态（0=正常）
-- description     : 描述
-- remark          : 备注
-- weixin_open_id  : 微信 OpenID
-- tenant_id       : 所属租户编号
-- delete_flag     : 删除标记
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
-- type            : 关联类型（UserRole=用户角色，RoleFunctions=角色菜单权限）
-- key_id          : 关联主体编号（UserRole 时为用户ID，RoleFunctions 时为角色ID）
-- value           : 关联目标编号列表（JSON 数组格式）
-- btn_str         : 按钮权限列表（JSON 格式，仅 RoleFunctions 使用）
-- tenant_id       : 所属租户编号
-- delete_flag     : 删除标记
--
-- 用户-角色关联：将用户 1 绑定到角色 1
INSERT INTO jsh_user_business (type, key_id, value, btn_str, tenant_id, delete_flag)
VALUES
    ('UserRole', '1', '[1]', NULL, 1, '0')
ON CONFLICT (COALESCE(tenant_id, 0), type, key_id) WHERE COALESCE(delete_flag, '0') != '1'
DO UPDATE SET value = EXCLUDED.value, btn_str = EXCLUDED.btn_str, delete_flag = EXCLUDED.delete_flag;

-- 角色-菜单权限关联：为角色 1 授予全部已启用菜单及按钮权限
INSERT INTO jsh_user_business (type, key_id, value, btn_str, tenant_id, delete_flag)
VALUES
    ('RoleFunctions', '1',
     '[1][13][14][15][16][18][21][22][23][24][25][26][31][32][33][38][40][41][44][59][194][195][197][198][199][200][201][202][203][204][205][206][207][208][209][210][211][212][217][218][220][225][226][227][228][229][232][233][234][235][236][237][239][241][242][243][244][245][246][247][248][258][259][260][261]',
     '[{"funId":13,"btnStr":"1"},{"funId":14,"btnStr":"1"},{"funId":243,"btnStr":"1"},{"funId":234,"btnStr":"1"},{"funId":236,"btnStr":"1"},{"funId":16,"btnStr":"1"},{"funId":18,"btnStr":"1"},{"funId":258,"btnStr":"1"},{"funId":22,"btnStr":"1"},{"funId":23,"btnStr":"1,3"},{"funId":220,"btnStr":"1"},{"funId":247,"btnStr":"1"},{"funId":25,"btnStr":"1,3"},{"funId":217,"btnStr":"1,3"},{"funId":218,"btnStr":"1"},{"funId":26,"btnStr":"1"},{"funId":194,"btnStr":"1"},{"funId":195,"btnStr":"1"},{"funId":31,"btnStr":"1"},{"funId":261,"btnStr":"1,2,3,7"},{"funId":241,"btnStr":"1,2,3,7"},{"funId":33,"btnStr":"1,2,3,7"},{"funId":199,"btnStr":"1,2,3,7"},{"funId":242,"btnStr":"1,2,3,7"},{"funId":41,"btnStr":"1,2,3,7"},{"funId":200,"btnStr":"1,2,3,7"},{"funId":210,"btnStr":"1,2,3,7"},{"funId":211,"btnStr":"1,2,3,7"},{"funId":197,"btnStr":"1,2,3,7"},{"funId":203,"btnStr":"1,2,3,7"},{"funId":204,"btnStr":"1,2,3,7"},{"funId":205,"btnStr":"1,2,3,7"},{"funId":206,"btnStr":"1,2,3,7"},{"funId":212,"btnStr":"1,2,3,7"},{"funId":201,"btnStr":"1,2,3,7"},{"funId":202,"btnStr":"1,2,3,7"},{"funId":40,"btnStr":"1,2,3,7"},{"funId":232,"btnStr":"1,2,3,7"},{"funId":233,"btnStr":"1,2,3,7"}]',
     1, '0')
ON CONFLICT (COALESCE(tenant_id, 0), type, key_id) WHERE COALESCE(delete_flag, '0') != '1'
DO UPDATE SET value = EXCLUDED.value, btn_str = EXCLUDED.btn_str, delete_flag = EXCLUDED.delete_flag;

-- ========================================
-- 租户系统配置表 jsh_system_config
-- ========================================
-- company_name              : 公司名称（部署后需修改）
-- depot_flag                : 仓库管理标记（0=按用户管理，1=按仓库管理）
-- customer_flag             : 客户管理标记（0=按用户管理，1=按客户管理）
-- minus_stock_flag          : 允许负库存（0=不允许，1=允许）
-- purchase_by_sale_flag     : 以销定购（0=关闭，1=开启）
-- multi_level_approval_flag : 多级审核（0=关闭，1=开启）
-- force_approval_flag       : 强制审核（0=关闭，1=开启）
-- update_unit_price_flag    : 允许修改单价（0=不允许，1=允许）
-- over_link_bill_flag       : 超源单（0=不允许，1=允许）
-- in_out_manage_flag        : 出入库管理（0=关闭，1=开启）
-- multi_account_flag        : 多账户管理（0=关闭，1=开启）
-- move_avg_price_flag       : 移动加权平均价（0=关闭，1=开启）
-- audit_print_flag          : 审核后才能打印（0=关闭，1=开启）
-- zero_change_amount_flag   : 允许零金额变动（0=不允许，1=允许）
-- customer_static_price_flag: 客户固定价格（0=关闭，1=开启）
-- material_price_tax_flag   : 商品价格含税标记（0=不含税，1=含税）
-- tenant_id                 : 所属租户编号
-- delete_flag               : 删除标记
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
SELECT setval('jsh_role_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM jsh_role), 1));
SELECT setval('jsh_user_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM jsh_user), 1));
SELECT setval('jsh_user_business_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM jsh_user_business), 1));
SELECT setval('jsh_system_config_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM jsh_system_config), 1));

COMMIT;
