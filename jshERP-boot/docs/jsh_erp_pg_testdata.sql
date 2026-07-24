-- ============================================================
-- jshERP 完整业务测试数据
-- 包含 2 个租户、完整进销存业务流程、每张表至少 10 条数据
-- 所有数据具有完整关联关系
-- ============================================================
SET client_encoding = 'UTF8';

-- ============================================================
-- 1. 租户 (jsh_tenant) - 2家公司的完整租户
-- ============================================================
INSERT INTO jsh_tenant (id, tenant_id, login_name, user_num_limit, type, enabled, create_time, expire_time, remark, tenant_code, delete_flag) VALUES
(1, 1001, '悦为智能', 50, '1', true, '2025-01-01 00:00:00', '2099-12-31 23:59:59', '悦为智能科技有限公司', 'A001', '0'),
(2, 1002, '普贸达', 30, '1', true, '2025-03-01 00:00:00', '2099-12-31 23:59:59', '普贸达进出口有限公司', 'B001', '0'),
(3, 1003, '模具公司', 20, '0', true, '2025-06-01 00:00:00', '2026-06-30 23:59:59', '精密模具制造有限公司', 'C001', '0')
ON CONFLICT (id) DO UPDATE SET
tenant_id = EXCLUDED.tenant_id, login_name = EXCLUDED.login_name, user_num_limit = EXCLUDED.user_num_limit,
type = EXCLUDED.type, enabled = EXCLUDED.enabled, create_time = EXCLUDED.create_time,
expire_time = EXCLUDED.expire_time, remark = EXCLUDED.remark, tenant_code = EXCLUDED.tenant_code, delete_flag = EXCLUDED.delete_flag;
SELECT setval('jsh_tenant_id_seq', GREATEST((SELECT MAX(id) FROM jsh_tenant), 1));

-- ============================================================
-- 2. 租户功能授权 (jsh_tenant_feature)
-- A001(悦为智能): 全模块
-- B001(普贸达): 采购+销售+库存（无财务）
-- C001(模具公司): 采购+库存
-- ============================================================
INSERT INTO jsh_tenant_feature (id, tenant_id, feature_id, enabled, create_time) VALUES
(1, 1001, 1, true, NOW()), (2, 1001, 2, true, NOW()), (3, 1001, 3, true, NOW()),
(4, 1001, 4, true, NOW()), (5, 1001, 5, true, NOW()),
(6, 1002, 1, true, NOW()), (7, 1002, 2, true, NOW()), (8, 1002, 3, true, NOW()),
(9, 1002, 4, true, NOW()),
(10, 1003, 1, true, NOW()), (11, 1003, 2, true, NOW()), (12, 1003, 3, true, NOW())
ON CONFLICT (id) DO UPDATE SET tenant_id = EXCLUDED.tenant_id, feature_id = EXCLUDED.feature_id, enabled = EXCLUDED.enabled;
SELECT setval('jsh_tenant_feature_id_seq', GREATEST((SELECT MAX(id) FROM jsh_tenant_feature), 1));

-- ============================================================
-- 3. 角色 (jsh_role) - 每个租户多个角色
-- ============================================================
INSERT INTO jsh_role (id, name, description, enabled, tenant_id, delete_flag) VALUES
-- A001 悦为智能
(1, '总经理', '公司最高权限', true, 1001, '0'),
(2, '采购经理', '负责采购管理', true, 1001, '0'),
(3, '销售经理', '负责销售管理', true, 1001, '0'),
(4, '仓库主管', '负责仓库管理', true, 1001, '0'),
(5, '财务主管', '负责财务管理', true, 1001, '0'),
-- B001 普贸达
(6, '总经理', '公司最高权限', true, 1002, '0'),
(7, '采购专员', '负责采购', true, 1002, '0'),
(8, '销售专员', '负责销售', true, 1002, '0'),
-- C001 模具公司
(9, '总经理', '公司最高权限', true, 1003, '0'),
(10, '生产主管', '负责生产管理', true, 1003, '0')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, enabled = EXCLUDED.enabled, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;
SELECT setval('jsh_role_id_seq', GREATEST((SELECT MAX(id) FROM jsh_role), 1));

-- ============================================================
-- 4. 用户 (jsh_user) - 每个租户多个用户
-- ============================================================
INSERT INTO jsh_user (id, username, login_name, password, leader_flag, position, department, phonenum, ismanager, isystem, status, tenant_id, delete_flag) VALUES
-- A001 悦为智能 (tenant_id=1001) - 租户拥有者 (id=tenantId)
(1001, '悦为管理员', 'ywadmin', 'e10adc3949ba59abbe56e057f20f883e', '1', '总经理', '总经办', '13800001000', 1, 0, 0, 1001, '0'),
-- A001 业务用户
(201, '张三', 'admin', 'e10adc3949ba59abbe56e057f20f883e', '1', '总经理', '总经办', '13800001001', 1, 0, 0, 1001, '0'),
(202, '李四', 'lisi', 'e10adc3949ba59abbe56e057f20f883e', '0', '采购经理', '采购部', '13800001002', 0, 0, 0, 1001, '0'),
(203, '王五', 'wangwu', 'e10adc3949ba59abbe56e057f20f883e', '0', '销售经理', '销售部', '13800001003', 0, 0, 0, 1001, '0'),
(204, '赵六', 'zhaoliu', 'e10adc3949ba59abbe56e057f20f883e', '0', '仓库主管', '仓储部', '13800001004', 0, 0, 0, 1001, '0'),
(205, '孙七', 'sunqi', 'e10adc3949ba59abbe56e057f20f883e', '0', '财务主管', '财务部', '13800001005', 0, 0, 0, 1001, '0'),
(206, '周八', 'zhouba', 'e10adc3949ba59abbe56e057f20f883e', '0', '采购专员', '采购部', '13800001006', 0, 0, 0, 1001, '0'),
(207, '吴九', 'wujiu', 'e10adc3949ba59abbe56e057f20f883e', '0', '销售代表', '销售部', '13800001007', 0, 0, 0, 1001, '0'),
(208, '郑十', 'zhengshi', 'e10adc3949ba59abbe56e057f20f883e', '0', '仓管员', '仓储部', '13800001008', 0, 0, 0, 1001, '0'),
(209, '钱十一', 'qian11', 'e10adc3949ba59abbe56e057f20f883e', '0', '会计', '财务部', '13800001009', 0, 0, 0, 1001, '0'),
(210, '刘十二', 'liu12', 'e10adc3949ba59abbe56e057f20f883e', '0', '实习生', '销售部', '13800001010', 0, 0, 0, 1001, '0'),
-- B001 普贸达 (tenant_id=1002) - 租户拥有者
(1002, '普贸达管理员', 'pmadmin', 'e10adc3949ba59abbe56e057f20f883e', '1', '总经理', '总经办', '13900002000', 1, 0, 0, 1002, '0'),
-- B001 业务用户
(301, '陈总', 'admin', 'e10adc3949ba59abbe56e057f20f883e', '1', '总经理', '总经办', '13900002001', 1, 0, 0, 1002, '0'),
(302, '黄经理', 'huangjl', 'e10adc3949ba59abbe56e057f20f883e', '0', '采购经理', '采购部', '13900002002', 0, 0, 0, 1002, '0'),
(303, '林经理', 'linjl', 'e10adc3949ba59abbe56e057f20f883e', '0', '销售经理', '销售部', '13900002003', 0, 0, 0, 1002, '0'),
(304, '何仓管', 'hecg', 'e10adc3949ba59abbe56e057f20f883e', '0', '仓库主管', '仓储部', '13900002004', 0, 0, 0, 1002, '0'),
(305, '罗采购', 'luocg', 'e10adc3949ba59abbe56e057f20f883e', '0', '采购专员', '采购部', '13900002005', 0, 0, 0, 1002, '0'),
(306, '梁销售', 'liangxs', 'e10adc3949ba59abbe56e057f20f883e', '0', '销售代表', '销售部', '13900002006', 0, 0, 0, 1002, '0'),
(307, '宋文员', 'songwy', 'e10adc3949ba59abbe56e057f20f883e', '0', '文员', '行政部', '13900002007', 0, 0, 0, 1002, '0'),
(308, '唐助理', 'tangzl', 'e10adc3949ba59abbe56e057f20f883e', '0', '总经理助理', '总经办', '13900002008', 0, 0, 0, 1002, '0'),
(309, '韩仓管', 'hancg', 'e10adc3949ba59abbe56e057f20f883e', '0', '仓管员', '仓储部', '13900002009', 0, 0, 0, 1002, '0'),
(310, '冯销售', 'fengxs', 'e10adc3949ba59abbe56e057f20f883e', '0', '销售助理', '销售部', '13900002010', 0, 0, 0, 1002, '0'),
-- C001 模具公司 (tenant_id=1003) - 租户拥有者
(1003, '模具管理员', 'mjadmin', 'e10adc3949ba59abbe56e057f20f883e', '1', '总经理', '总经办', '13700003000', 1, 0, 0, 1003, '0'),
-- C001 业务用户
(401, '王总', 'admin', 'e10adc3949ba59abbe56e057f20f883e', '1', '总经理', '总经办', '13700003001', 1, 0, 0, 1003, '0'),
(402, '刘工', 'liugong', 'e10adc3949ba59abbe56e057f20f883e', '0', '生产主管', '生产部', '13700003002', 0, 0, 0, 1003, '0'),
(403, '张采购', 'zhangcg', 'e10adc3949ba59abbe56e057f20f883e', '0', '采购专员', '采购部', '13700003003', 0, 0, 0, 1003, '0'),
(404, '李仓管', 'licg', 'e10adc3949ba59abbe56e057f20f883e', '0', '仓管员', '仓储部', '13700003004', 0, 0, 0, 1003, '0'),
(405, '赵质检', 'zhaozj', 'e10adc3949ba59abbe56e057f20f883e', '0', '质检员', '品质部', '13700003005', 0, 0, 0, 1003, '0'),
(406, '钱技工', 'qianjg', 'e10adc3949ba59abbe56e057f20f883e', '0', '技术员', '技术部', '13700003006', 0, 0, 0, 1003, '0'),
(407, '孙生产', 'sunsc', 'e10adc3949ba59abbe56e057f20f883e', '0', '生产工人', '生产部', '13700003007', 0, 0, 0, 1003, '0'),
(408, '周计划', 'zhoujh', 'e10adc3949ba59abbe56e057f20f883e', '0', '计划员', '生产部', '13700003008', 0, 0, 0, 1003, '0'),
(409, '吴文员', 'wuwy', 'e10adc3949ba59abbe56e057f20f883e', '0', '文员', '行政部', '13700003009', 0, 0, 0, 1003, '0'),
(410, '郑助理', 'zhengzl', 'e10adc3949ba59abbe56e057f20f883e', '0', '总经理助理', '总经办', '13700003010', 0, 0, 0, 1003, '0')
ON CONFLICT (id) DO UPDATE SET username = EXCLUDED.username, login_name = EXCLUDED.login_name, password = EXCLUDED.password,
leader_flag = EXCLUDED.leader_flag, position = EXCLUDED.position, department = EXCLUDED.department, phonenum = EXCLUDED.phonenum,
ismanager = EXCLUDED.ismanager, isystem = EXCLUDED.isystem, status = EXCLUDED.status, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;
SELECT setval('jsh_user_id_seq', GREATEST((SELECT MAX(id) FROM jsh_user), 1));

-- ============================================================
-- 5. 用户-角色关系 (jsh_user_business)
-- ============================================================
INSERT INTO jsh_user_business (id, type, key_id, value, btn_str, tenant_id, delete_flag) VALUES
-- A001 租户拥有者角色
(1001, 'UserRole', '1001', '[1]', NULL, 1001, '0'),
-- A001 用户角色
(201, 'UserRole', '201', '[1]', NULL, 1001, '0'),
(202, 'UserRole', '202', '[2]', NULL, 1001, '0'),
(203, 'UserRole', '203', '[3]', NULL, 1001, '0'),
(204, 'UserRole', '204', '[4]', NULL, 1001, '0'),
(205, 'UserRole', '205', '[5]', NULL, 1001, '0'),
(206, 'UserRole', '206', '[2]', NULL, 1001, '0'),
(207, 'UserRole', '207', '[3]', NULL, 1001, '0'),
(208, 'UserRole', '208', '[4]', NULL, 1001, '0'),
(209, 'UserRole', '209', '[5]', NULL, 1001, '0'),
(210, 'UserRole', '210', '[3]', NULL, 1001, '0'),
-- B001 租户拥有者角色
(1002, 'UserRole', '1002', '[6]', NULL, 1002, '0'),
-- B001 用户角色
(301, 'UserRole', '301', '[6]', NULL, 1002, '0'),
(302, 'UserRole', '302', '[7]', NULL, 1002, '0'),
(303, 'UserRole', '303', '[8]', NULL, 1002, '0'),
(304, 'UserRole', '304', '[6]', NULL, 1002, '0'),
(305, 'UserRole', '305', '[7]', NULL, 1002, '0'),
(306, 'UserRole', '306', '[8]', NULL, 1002, '0'),
(307, 'UserRole', '307', '[7]', NULL, 1002, '0'),
(308, 'UserRole', '308', '[6]', NULL, 1002, '0'),
(309, 'UserRole', '309', '[6]', NULL, 1002, '0'),
(310, 'UserRole', '310', '[8]', NULL, 1002, '0'),
-- C001 租户拥有者角色
(1003, 'UserRole', '1003', '[9]', NULL, 1003, '0'),
-- C001 用户角色
(401, 'UserRole', '401', '[9]', NULL, 1003, '0'),
(402, 'UserRole', '402', '[10]', NULL, 1003, '0'),
(403, 'UserRole', '403', '[9]', NULL, 1003, '0'),
(404, 'UserRole', '404', '[10]', NULL, 1003, '0'),
(405, 'UserRole', '405', '[10]', NULL, 1003, '0'),
(406, 'UserRole', '406', '[10]', NULL, 1003, '0'),
(407, 'UserRole', '407', '[10]', NULL, 1003, '0'),
(408, 'UserRole', '408', '[10]', NULL, 1003, '0'),
(409, 'UserRole', '409', '[9]', NULL, 1003, '0'),
(410, 'UserRole', '410', '[9]', NULL, 1003, '0')
ON CONFLICT (id) DO UPDATE SET type = EXCLUDED.type, key_id = EXCLUDED.key_id, value = EXCLUDED.value, btn_str = EXCLUDED.btn_str, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;

-- 角色功能权限 (RoleFunctions)
INSERT INTO jsh_user_business (id, type, key_id, value, btn_str, tenant_id, delete_flag) VALUES
(411, 'RoleFunctions', '1', '[1][13][14][15][16][18][21][22][23][24][25][26][31][32][33][38][40][41][44][59][194][195][197][198][199][200][201][202][203][204][205][206][207][208][209][210][211][212][217][218][220][225][226][227][228][229][232][233][234][235][236][237][239][241][242][243][244][245][246][247][248][258][259][260][261]', NULL, 1001, '0'),
(412, 'RoleFunctions', '2', '[1][14][21][22][23][24][25][26][31][32][33][199][241][261]', NULL, 1001, '0'),
(413, 'RoleFunctions', '3', '[1][14][21][22][23][24][217][218][38][41][200][242][225][210][211][198][59][208][209]', NULL, 1001, '0'),
(414, 'RoleFunctions', '4', '[1][14][21][22][23][26][239][40][201][202][232][233][198][226][227][228][229][244][246]', NULL, 1001, '0'),
(415, 'RoleFunctions', '5', '[1][14][194][195][44][197][203][204][205][206][212][198][207]', NULL, 1001, '0'),
(416, 'RoleFunctions', '6', '[1][13][14][15][16][18][21][22][23][24][25][26][31][32][33][38][40][41][59][198][199][200][201][202][207][208][209][210][211][217][218][220][225][226][227][228][229][232][233][234][235][236][237][239][241][242][243][244][245][246][247][248][259][260][261]', NULL, 1002, '0'),
(417, 'RoleFunctions', '7', '[1][14][21][22][23][24][25][26][31][32][33][199][241][261]', NULL, 1002, '0'),
(418, 'RoleFunctions', '8', '[1][14][21][22][23][24][217][218][38][41][200][242][225][210][211]', NULL, 1002, '0'),
(419, 'RoleFunctions', '9', '[1][13][14][15][16][18][21][22][23][24][25][26][31][32][33][198][199][201][208][226][227][228][229][232][233][239][241][244][246][261]', NULL, 1003, '0'),
(420, 'RoleFunctions', '10', '[1][14][21][22][23][24][25][26][31][32][33][199][241][239][201][202][232][233][261]', NULL, 1003, '0')
ON CONFLICT (id) DO UPDATE SET type = EXCLUDED.type, key_id = EXCLUDED.key_id, value = EXCLUDED.value, btn_str = EXCLUDED.btn_str, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;
SELECT setval('jsh_user_business_id_seq', GREATEST((SELECT MAX(id) FROM jsh_user_business), 1));

-- ============================================================
-- 6. 部门 (jsh_organization) - 每个租户 3-4 个部门
-- ============================================================
INSERT INTO jsh_organization (id, org_no, org_abr, parent_id, sort, remark, tenant_id, delete_flag) VALUES
-- A001
(1, '001', '总经办', 0, '1', '总经理办公室', 1001, '0'),
(2, '002', '采购部', 0, '2', '采购管理部门', 1001, '0'),
(3, '003', '销售部', 0, '3', '销售管理部门', 1001, '0'),
(4, '004', '仓储部', 0, '4', '仓储管理部门', 1001, '0'),
(5, '005', '财务部', 0, '5', '财务管理部门', 1001, '0'),
-- B001
(6, '001', '总经办', 0, '1', '总经理办公室', 1002, '0'),
(7, '002', '采购部', 0, '2', '采购管理部门', 1002, '0'),
(8, '003', '销售部', 0, '3', '销售管理部门', 1002, '0'),
(9, '004', '仓储部', 0, '4', '仓储管理部门', 1002, '0'),
-- C001
(10, '001', '总经办', 0, '1', '总经理办公室', 1003, '0'),
(11, '002', '生产部', 0, '2', '生产管理部门', 1003, '0'),
(12, '003', '采购部', 0, '3', '采购管理部门', 1003, '0')
ON CONFLICT (id) DO UPDATE SET org_no = EXCLUDED.org_no, org_abr = EXCLUDED.org_abr, parent_id = EXCLUDED.parent_id, sort = EXCLUDED.sort, remark = EXCLUDED.remark, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;
SELECT setval('jsh_organization_id_seq', GREATEST((SELECT MAX(id) FROM jsh_organization), 1));

-- ============================================================
-- 7. 仓库 (jsh_depot) - 每个租户 2-3 个仓库
-- ============================================================
INSERT INTO jsh_depot (id, name, type, sort, enabled, is_default, tenant_id, delete_flag) VALUES
-- A001
(1, '主仓库', 0, '1', true, true, 1001, '0'),
(2, '原材料仓', 0, '2', true, false, 1001, '0'),
(3, '成品仓', 0, '3', true, false, 1001, '0'),
-- B001
(4, '总仓', 0, '1', true, true, 1002, '0'),
(5, '深圳仓', 0, '2', true, false, 1002, '0'),
(6, '广州仓', 0, '3', true, false, 1002, '0'),
-- C001
(7, '原材料仓', 0, '1', true, true, 1003, '0'),
(8, '成品仓', 0, '2', true, false, 1003, '0')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, type = EXCLUDED.type, sort = EXCLUDED.sort, enabled = EXCLUDED.enabled, is_default = EXCLUDED.is_default, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;
SELECT setval('jsh_depot_id_seq', GREATEST((SELECT MAX(id) FROM jsh_depot), 1));

-- ============================================================
-- 8. 结算账户 (jsh_account)
-- ============================================================
INSERT INTO jsh_account (id, name, serial_no, initial_amount, current_amount, enabled, is_default, tenant_id, delete_flag) VALUES
(1, '对公账户', 'BANK-A001', 100000, 100000, true, true, 1001, '0'),
(2, '现金账户', 'CASH-A001', 50000, 50000, true, false, 1001, '0'),
(3, '支付宝', 'ALI-A001', 30000, 30000, true, false, 1001, '0'),
(4, '对公账户', 'BANK-B001', 200000, 200000, true, true, 1002, '0'),
(5, '现金账户', 'CASH-B001', 20000, 20000, true, false, 1002, '0'),
(6, '微信支付', 'WX-B001', 15000, 15000, true, false, 1002, '0'),
(7, '对公账户', 'BANK-C001', 80000, 80000, true, true, 1003, '0'),
(8, '现金账户', 'CASH-C001', 10000, 10000, true, false, 1003, '0')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, serial_no = EXCLUDED.serial_no, initial_amount = EXCLUDED.initial_amount, current_amount = EXCLUDED.current_amount, enabled = EXCLUDED.enabled, is_default = EXCLUDED.is_default, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;
SELECT setval('jsh_account_id_seq', GREATEST((SELECT MAX(id) FROM jsh_account), 1));

-- ============================================================
-- 9. 收支项目 (jsh_in_out_item)
-- ============================================================
INSERT INTO jsh_in_out_item (id, name, type, tenant_id, delete_flag) VALUES
(1, '快递费', '支出', 1001, '0'), (2, '房租收入', '收入', 1001, '0'), (3, '利息收入', '收入', 1001, '0'),
(4, '水电费', '支出', 1001, '0'), (5, '办公用品', '支出', 1001, '0'),
(6, '快递费', '支出', 1002, '0'), (7, '广告费', '支出', 1002, '0'), (8, '佣金收入', '收入', 1002, '0'),
(9, '运费', '支出', 1003, '0'), (10, '模具收入', '收入', 1003, '0')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, type = EXCLUDED.type, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;
SELECT setval('jsh_in_out_item_id_seq', GREATEST((SELECT MAX(id) FROM jsh_in_out_item), 1));

-- ============================================================
-- 10. 经手人 (jsh_person) - 每个租户 3-4 人
-- ============================================================
INSERT INTO jsh_person (id, name, type, tenant_id, delete_flag) VALUES
(1, '张三', '仓管员', 1001, '0'),
(2, '李四', '销售员', 1001, '0'),
(3, '王五', '销售员', 1001, '0'),
(4, '赵六', '仓管员', 1001, '0'),
(5, '陈总', '仓管员', 1002, '0'),
(6, '黄经理', '销售员', 1002, '0'),
(7, '林经理', '销售员', 1002, '0'),
(8, '何仓管', '仓管员', 1002, '0'),
(9, '王总', '仓管员', 1003, '0'),
(10, '张采购', '财务员', 1003, '0')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, type = EXCLUDED.type, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;
SELECT setval('jsh_person_id_seq', GREATEST((SELECT MAX(id) FROM jsh_person), 1));

-- ============================================================
-- 11. 供应商/客户 (jsh_supplier) - 每个租户 5 供应商 + 5 客户
-- ============================================================
INSERT INTO jsh_supplier (id, supplier, contacts, phone_num, type, email, description, enabled, isystem, tenant_id, delete_flag) VALUES
-- A001 供应商
(1, '深圳华强电子', '张经理', '0755-11111111', '供应商', 'zhang@huaqiang.com', '电子元器件供应商', true, 0, 1001, '0'),
(2, '东莞五金制品厂', '李总', '0769-22222222', '供应商', 'li@wujin.com', '五金配件供应商', true, 0, 1001, '0'),
(3, '佛山塑料有限公司', '王经理', '0757-33333333', '供应商', 'wang@suliao.com', '塑料原料供应商', true, 0, 1001, '0'),
(4, '广州包装材料公司', '赵总', '020-44444444', '供应商', 'zhao@baozhuang.com', '包装材料供应商', true, 0, 1001, '0'),
(5, '苏州精密机械', '孙经理', '0512-55555555', '供应商', 'sun@jingmi.com', '精密零件供应商', true, 0, 1001, '0'),
-- A001 客户
(6, '北京科技有限公司', '周总', '010-66666666', '客户', 'zhou@beijing.com', '北京大客户', true, 0, 1001, '0'),
(7, '上海贸易有限公司', '吴经理', '021-77777777', '客户', 'wu@shanghai.com', '上海经销商', true, 0, 1001, '0'),
(8, '杭州电商有限公司', '郑总', '0571-88888888', '客户', 'zheng@hangzhou.com', '电商平台客户', true, 0, 1001, '0'),
(9, '成都批发商行', '钱老板', '028-99999999', '客户', 'qian@chengdu.com', '西南区域批发', true, 0, 1001, '0'),
(10, '武汉零售连锁', '刘店长', '027-10101010', '客户', 'liu@wuhan.com', '华中区域零售', true, 0, 1001, '0'),
-- B001 供应商
(11, '香港国际商贸', '黄总', '852-11111111', '供应商', 'huang@hk.com', '进口商品供应商', true, 0, 1002, '0'),
(12, '义乌小商品城', '林老板', '0579-22222222', '供应商', 'lin@yiwu.com', '小商品批发', true, 0, 1002, '0'),
(13, '汕头玩具厂', '何总', '0754-33333333', '供应商', 'he@shantou.com', '玩具供应商', true, 0, 1002, '0'),
(14, '温州鞋业集团', '罗经理', '0777-44444444', '供应商', 'luo@wenzhou.com', '鞋类供应商', true, 0, 1002, '0'),
(15, '泉州服装厂', '梁总', '0595-55555555', '供应商', 'liang@quanzhou.com', '服装供应商', true, 0, 1002, '0'),
-- B001 客户
(16, 'Amazon卖家A', '陈买手', '136-66666666', '客户', 'chen@amazon.com', '跨境电商客户', true, 0, 1002, '0'),
(17, 'Shopee卖家B', '黄买手', '137-77777777', '客户', 'huang@shopee.com', '东南亚电商', true, 0, 1002, '0'),
(18, '中东贸易公司', 'Ali', '971-88888888', '客户', 'ali@dubai.com', '中东客户', true, 0, 1002, '0'),
(19, '韩国进口商', 'Kim', '82-99999999', '客户', 'kim@korea.com', '韩国客户', true, 0, 1002, '0'),
(20, '日本代理商', 'Tanaka', '81-10101010', '客户', 'tanaka@japan.com', '日本客户', true, 0, 1002, '0'),
-- C001 供应商
(21, '宝钢特钢', '王采购', '021-11111111', '供应商', 'wang@baosteel.com', '钢材供应商', true, 0, 1003, '0'),
(22, '株洲硬质合金', '李经理', '0731-22222222', '供应商', 'li@zhuzhou.com', '合金材料', true, 0, 1003, '0'),
(23, '昆山模具配件', '张总', '0512-33333333', '供应商', 'zhang@kunshan.com', '模具配件', true, 0, 1003, '0'),
(24, '深圳CNC加工厂', '赵厂长', '0755-44444444', '供应商', 'zhao@cnc.com', 'CNC加工', true, 0, 1003, '0'),
(25, '东莞热处理厂', '孙总', '0769-55555555', '供应商', 'sun@heat.com', '热处理加工', true, 0, 1003, '0'),
-- C001 客户
(26, '美的集团', '刘采购', '0757-66666666', '客户', 'liu@midea.com', '家电大客户', true, 0, 1003, '0'),
(27, '比亚迪汽车', '周工程师', '0755-77777777', '客户', 'zhou@byd.com', '汽车零部件', true, 0, 1003, '0'),
(28, '格力电器', '吴采购', '0756-88888888', '客户', 'wu@gree.com', '家电客户', true, 0, 1003, '0'),
(29, '华为技术', '郑工程师', '0755-99999999', '客户', 'zheng@huawei.com', '通信设备', true, 0, 1003, '0'),
(30, '富士康科技', '钱经理', '0755-12121212', '客户', 'qian@foxconn.com', '代工客户', true, 0, 1003, '0')
ON CONFLICT (id) DO UPDATE SET supplier = EXCLUDED.supplier, contacts = EXCLUDED.contacts, phone_num = EXCLUDED.phone_num, type = EXCLUDED.type, email = EXCLUDED.email, description = EXCLUDED.description, enabled = EXCLUDED.enabled, isystem = EXCLUDED.isystem, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;
SELECT setval('jsh_supplier_id_seq', GREATEST((SELECT MAX(id) FROM jsh_supplier), 1));

-- ============================================================
-- 12. 多单位 (jsh_unit)
-- ============================================================
INSERT INTO jsh_unit (id, name, basic_unit, other_unit, other_unit_two, tenant_id, delete_flag) VALUES
(1, '个/箱', '个', '箱', NULL, 1001, '0'),
(2, '个/盒/箱', '个', '盒', '箱', 1001, '0'),
(3, '瓶/箱', '瓶', '箱', NULL, 1001, '0'),
(4, 'kg/吨', 'kg', '吨', NULL, 1001, '0'),
(5, '件/箱', '件', '箱', NULL, 1002, '0'),
(6, '双/箱', '双', '箱', NULL, 1002, '0'),
(7, '个/包', '个', '包', NULL, 1002, '0'),
(8, 'kg/吨', 'kg', '吨', NULL, 1003, '0'),
(9, '件/箱', '件', '箱', NULL, 1003, '0'),
(10, '套/箱', '套', '箱', NULL, 1003, '0')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, basic_unit = EXCLUDED.basic_unit, other_unit = EXCLUDED.other_unit, other_unit_two = EXCLUDED.other_unit_two, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;
SELECT setval('jsh_unit_id_seq', GREATEST((SELECT MAX(id) FROM jsh_unit), 1));

-- ============================================================
-- 13. 商品类别 (jsh_material_category)
-- ============================================================
INSERT INTO jsh_material_category (id, name, parent_id, sort, tenant_id, delete_flag) VALUES
-- A001
(1, '电子元器件', 0, '1', 1001, '0'),
(2, '五金配件', 0, '2', 1001, '0'),
(3, '包装材料', 0, '3', 1001, '0'),
-- B001
(4, '日用百货', 0, '1', 1002, '0'),
(5, '服装配饰', 0, '2', 1002, '0'),
(6, '玩具礼品', 0, '3', 1002, '0'),
-- C001
(7, '模具钢材', 0, '1', 1003, '0'),
(8, '标准件', 0, '2', 1003, '0'),
(9, '刀具工具', 0, '3', 1003, '0'),
(10, '成品模具', 0, '4', 1003, '0')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, parent_id = EXCLUDED.parent_id, sort = EXCLUDED.sort, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;
SELECT setval('jsh_material_category_id_seq', GREATEST((SELECT MAX(id) FROM jsh_material_category), 1));

-- ============================================================
-- 14. 商品 (jsh_material) - 每个租户 10+ 个商品
-- ============================================================
INSERT INTO jsh_material (id, name, standard, model, unit, enabled, enable_serial_number, enable_batch_number, tenant_id, delete_flag) VALUES
-- A001 电子元器件
(1, '电阻10K', '0805', '10KΩ±5%', '个', true, '0', '0', 1001, '0'),
(2, '电容100uF', '1206', '100uF/25V', '个', true, '0', '0', 1001, '0'),
(3, 'LED灯珠', '5050', '白光6000K', '个', true, '0', '0', 1001, '0'),
-- A001 五金配件
(4, 'M3螺丝', '304不锈钢', 'M3x10', '个', true, '0', '0', 1001, '0'),
(5, 'M4螺母', '304不锈钢', 'M4', '个', true, '0', '0', 1001, '0'),
(6, '弹簧垫圈', '65Mn', 'M6', '个', true, '0', '0', 1001, '0'),
-- A001 包装材料
(7, '纸箱40x30x20', '三层瓦楞', '40x30x20cm', '个', true, '0', '0', 1001, '0'),
(8, '气泡膜', '双层', '宽30cm', '米', true, '0', '0', 1001, '0'),
(9, '封箱胶带', '透明', '48mmx100m', '卷', true, '0', '0', 1001, '0'),
(10, '防静电袋', '粉红色', '20x30cm', '个', true, '0', '0', 1001, '0'),
-- B001 日用百货
(11, '保温杯', '304不锈钢', '500ml', '个', true, '0', '0', 1002, '0'),
(12, '雨伞', '折叠', '三折8骨', '把', true, '0', '0', 1002, '0'),
(13, '手机壳', '硅胶', 'iPhone15', '个', true, '0', '0', 1002, '0'),
-- B001 服装配饰
(14, 'T恤', '纯棉', '圆领M码', '件', true, '0', '0', 1002, '0'),
(15, '牛仔裤', '棉弹', '直筒30码', '条', true, '0', '0', 1002, '0'),
(16, '棒球帽', '涤纶', '可调节', '顶', true, '0', '0', 1002, '0'),
-- B001 玩具礼品
(17, '毛绒玩具', 'PP棉', '中号30cm', '个', true, '0', '0', 1002, '0'),
(18, '益智积木', 'ABS', '100片装', '盒', true, '0', '0', 1002, '0'),
(19, '遥控汽车', '充电', '1:16比例', '个', true, '0', '0', 1002, '0'),
(20, '文具套装', '多功能', '24件套', '套', true, '0', '0', 1002, '0'),
-- C001 模具钢材
(21, 'P20预硬钢', 'HB280-330', '200x300x400', 'kg', true, '0', '0', 1003, '0'),
(22, 'H13热作钢', 'HRC48-52', '150x200x300', 'kg', true, '0', '0', 1003, '0'),
(23, 'S136镜面钢', 'HRC48-52', '100x150x200', 'kg', true, '0', '0', 1003, '0'),
-- C001 标准件
(24, '导柱', 'SUJ2', 'Φ20x150', '根', true, '0', '0', 1003, '0'),
(25, '导套', 'SUJ2', 'Φ20x80', '根', true, '0', '0', 1003, '0'),
(26, '顶针', 'SKD61', 'Φ3x100', '根', true, '0', '0', 1003, '0'),
-- C001 刀具工具
(27, '立铣刀', '硬质合金', 'Φ6x50', '把', true, '0', '0', 1003, '0'),
(28, '钻头', '高速钢', 'Φ5x80', '把', true, '0', '0', 1003, '0'),
-- C001 成品模具
(29, '手机壳注塑模', '一出四', '150x200x250', '套', true, '0', '0', 1003, '0'),
(30, '连接器冲压模', '连续模', '200x300x150', '套', true, '0', '0', 1003, '0')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, standard = EXCLUDED.standard, model = EXCLUDED.model, unit = EXCLUDED.unit, enabled = EXCLUDED.enabled, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;
SELECT setval('jsh_material_id_seq', GREATEST((SELECT MAX(id) FROM jsh_material), 1));

-- ============================================================
-- 15. 商品价格 (jsh_material_extend) - 每个商品的定价
-- ============================================================
INSERT INTO jsh_material_extend (id, material_id, bar_code, commodity_unit, purchase_decimal, commodity_decimal, wholesale_decimal, low_decimal, default_flag, tenant_id, delete_flag) VALUES
-- A001
(1, 1, 'R-10K-0805', '个', 0.01, 0.02, 0.015, 0.008, '1', 1001, '0'),
(2, 2, 'C-100U-1206', '个', 0.05, 0.10, 0.08, 0.04, '1', 1001, '0'),
(3, 3, 'LED-5050-W', '个', 0.08, 0.15, 0.12, 0.06, '1', 1001, '0'),
(4, 4, 'SC-M3x10-304', '个', 0.02, 0.05, 0.04, 0.015, '1', 1001, '0'),
(5, 5, 'NT-M4-304', '个', 0.01, 0.03, 0.02, 0.008, '1', 1001, '0'),
(6, 6, 'WS-M6-65Mn', '个', 0.03, 0.06, 0.05, 0.02, '1', 1001, '0'),
(7, 7, 'BOX-403020', '个', 2.50, 4.00, 3.50, 2.00, '1', 1001, '0'),
(8, 8, 'BUB-30-DL', '米', 0.30, 0.60, 0.50, 0.25, '1', 1001, '0'),
(9, 9, 'TAPE-48-100', '卷', 3.00, 5.00, 4.50, 2.50, '1', 1001, '0'),
(10, 10, 'ESD-2030', '个', 0.10, 0.20, 0.15, 0.08, '1', 1001, '0'),
-- B001
(11, 11, 'CUP-500-304', '个', 15.00, 35.00, 28.00, 12.00, '1', 1002, '0'),
(12, 12, 'UMB-3F-8B', '把', 8.00, 18.00, 15.00, 6.00, '1', 1002, '0'),
(13, 13, 'CASE-I15-SI', '个', 2.00, 8.00, 6.00, 1.50, '1', 1002, '0'),
(14, 14, 'TSH-COT-M', '件', 12.00, 35.00, 28.00, 10.00, '1', 1002, '0'),
(15, 15, 'JNS-ST-30', '条', 25.00, 65.00, 50.00, 20.00, '1', 1002, '0'),
(16, 16, 'CAP-BB-ADJ', '顶', 5.00, 15.00, 12.00, 4.00, '1', 1002, '0'),
(17, 17, 'TOY-PP-30', '个', 8.00, 25.00, 20.00, 6.00, '1', 1002, '0'),
(18, 18, 'BLK-ABS-100', '盒', 15.00, 38.00, 30.00, 12.00, '1', 1002, '0'),
(19, 19, 'CAR-RC-116', '个', 25.00, 68.00, 55.00, 20.00, '1', 1002, '0'),
(20, 20, 'STN-24', '套', 10.00, 28.00, 22.00, 8.00, '1', 1002, '0'),
-- C001
(21, 21, 'STL-P20-200', 'kg', 18.00, 25.00, 22.00, 15.00, '1', 1003, '0'),
(22, 22, 'STL-H13-150', 'kg', 28.00, 40.00, 35.00, 22.00, '1', 1003, '0'),
(23, 23, 'STL-S136-100', 'kg', 45.00, 65.00, 55.00, 38.00, '1', 1003, '0'),
(24, 24, 'PIN-SUJ2-20', '根', 8.00, 15.00, 12.00, 6.00, '1', 1003, '0'),
(25, 25, 'BUSH-SUJ2-20', '根', 6.00, 12.00, 10.00, 5.00, '1', 1003, '0'),
(26, 26, 'EP-SKD61-3', '根', 1.50, 3.00, 2.50, 1.20, '1', 1003, '0'),
(27, 27, 'MILL-C6-50', '把', 12.00, 25.00, 20.00, 10.00, '1', 1003, '0'),
(28, 28, 'DRILL-HSS-5', '把', 3.00, 8.00, 6.00, 2.50, '1', 1003, '0'),
(29, 29, 'MOLD-PH-150', '套', 8000.00, 15000.00, 12000.00, 6500.00, '1', 1003, '0'),
(30, 30, 'MOLD-CONN-200', '套', 12000.00, 22000.00, 18000.00, 10000.00, '1', 1003, '0')
ON CONFLICT (id) DO UPDATE SET material_id = EXCLUDED.material_id, bar_code = EXCLUDED.bar_code, commodity_unit = EXCLUDED.commodity_unit, purchase_decimal = EXCLUDED.purchase_decimal, commodity_decimal = EXCLUDED.commodity_decimal, wholesale_decimal = EXCLUDED.wholesale_decimal, low_decimal = EXCLUDED.low_decimal, default_flag = EXCLUDED.default_flag, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;
SELECT setval('jsh_material_extend_id_seq', GREATEST((SELECT MAX(id) FROM jsh_material_extend), 1));

-- ============================================================
-- 16. 当前库存 (jsh_material_current_stock) - 每个商品在主仓库的库存
-- ============================================================
INSERT INTO jsh_material_current_stock (id, material_id, depot_id, current_number, current_unit_price, tenant_id, delete_flag) VALUES
-- A001 主仓库库存
(1, 1, 1, 5000, 0.01, 1001, '0'), (2, 2, 1, 2000, 0.05, 1001, '0'),
(3, 3, 1, 8000, 0.08, 1001, '0'), (4, 4, 1, 20000, 0.02, 1001, '0'),
(5, 5, 1, 15000, 0.01, 1001, '0'), (6, 6, 1, 10000, 0.03, 1001, '0'),
(7, 7, 1, 500, 2.50, 1001, '0'), (8, 8, 1, 1000, 0.30, 1001, '0'),
(9, 9, 1, 200, 3.00, 1001, '0'), (10, 10, 1, 3000, 0.10, 1001, '0'),
-- B001 总仓库存
(11, 11, 4, 300, 15.00, 1002, '0'), (12, 12, 4, 500, 8.00, 1002, '0'),
(13, 13, 4, 1500, 2.00, 1002, '0'), (14, 14, 4, 800, 12.00, 1002, '0'),
(15, 15, 4, 400, 25.00, 1002, '0'), (16, 16, 4, 600, 5.00, 1002, '0'),
(17, 17, 4, 200, 8.00, 1002, '0'), (18, 18, 4, 150, 15.00, 1002, '0'),
(19, 19, 4, 100, 25.00, 1002, '0'), (20, 20, 4, 250, 10.00, 1002, '0'),
-- C001 原材料仓库存
(21, 21, 7, 2000, 18.00, 1003, '0'), (22, 22, 7, 1000, 28.00, 1003, '0'),
(23, 23, 7, 800, 45.00, 1003, '0'), (24, 24, 7, 200, 8.00, 1003, '0'),
(25, 25, 7, 200, 6.00, 1003, '0'), (26, 26, 7, 500, 1.50, 1003, '0'),
(27, 27, 7, 50, 12.00, 1003, '0'), (28, 28, 7, 80, 3.00, 1003, '0'),
(29, 29, 8, 3, 8000.00, 1003, '0'), (30, 30, 8, 2, 12000.00, 1003, '0')
ON CONFLICT (id) DO UPDATE SET material_id = EXCLUDED.material_id, depot_id = EXCLUDED.depot_id, current_number = EXCLUDED.current_number, current_unit_price = EXCLUDED.current_unit_price, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;
SELECT setval('jsh_material_current_stock_id_seq', GREATEST((SELECT MAX(id) FROM jsh_material_current_stock), 1));

-- ============================================================
-- 17. 采购入库单 (jsh_depot_head + jsh_depot_item) - A001 的 10 笔采购
-- ============================================================
INSERT INTO jsh_depot_head (id, type, sub_type, number, create_time, oper_time, organ_id, creator, account_id, change_amount, total_price, pay_type, status, sales_man, tenant_id, delete_flag) VALUES
(1, '入库', '采购', 'PUR-2025-0001', '2025-01-15 10:00:00', '2025-01-15 10:00:00', 1, 202, 1, 1500, 1500, '现付', '0', '张三', 1001, '0'),
(2, '入库', '采购', 'PUR-2025-0002', '2025-02-10 09:30:00', '2025-02-10 09:30:00', 2, 202, 1, 3000, 3000, '现付', '0', '李四', 1001, '0'),
(3, '入库', '采购', 'PUR-2025-0003', '2025-03-05 14:00:00', '2025-03-05 14:00:00', 3, 206, 1, 5000, 5000, '月结', '0', '张三', 1001, '0'),
(4, '入库', '采购', 'PUR-2025-0004', '2025-04-20 11:00:00', '2025-04-20 11:00:00', 4, 202, 2, 800, 800, '现付', '0', '李四', 1001, '0'),
(5, '入库', '采购', 'PUR-2025-0005', '2025-05-18 15:30:00', '2025-05-18 15:30:00', 1, 206, 1, 2000, 2000, '月结', '0', '张三', 1001, '0'),
(6, '入库', '采购', 'PUR-2025-0006', '2025-06-12 08:00:00', '2025-06-12 08:00:00', 2, 202, 1, 1200, 1200, '现付', '0', '李四', 1001, '0'),
(7, '入库', '采购', 'PUR-2025-0007', '2025-07-01 10:30:00', '2025-07-01 10:30:00', 5, 206, 1, 6000, 6000, '月结', '0', '张三', 1001, '0'),
(8, '入库', '采购', 'PUR-2025-0008', '2025-07-10 14:00:00', '2025-07-10 14:00:00', 3, 202, 2, 450, 450, '现付', '0', '李四', 1001, '0'),
(9, '入库', '采购', 'PUR-2025-0009', '2025-07-15 09:00:00', '2025-07-15 09:00:00', 1, 206, 1, 900, 900, '现付', '0', '张三', 1001, '0'),
(10, '入库', '采购', 'PUR-2025-0010', '2025-07-20 16:00:00', '2025-07-20 16:00:00', 4, 202, 1, 3500, 3500, '月结', '0', '李四', 1001, '0')
ON CONFLICT (id) DO UPDATE SET type = EXCLUDED.type, sub_type = EXCLUDED.sub_type, number = EXCLUDED.number, organ_id = EXCLUDED.organ_id, creator = EXCLUDED.creator, total_price = EXCLUDED.total_price, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;

INSERT INTO jsh_depot_item (id, header_id, material_id, material_extend_id, material_unit, oper_number, unit_price, all_price, depot_id, tenant_id, delete_flag) VALUES
(1, 1, 1, 1, '个', 100000, 0.01, 1000, 1, 1001, '0'),
(2, 1, 2, 2, '个', 10000, 0.05, 500, 1, 1001, '0'),
(3, 2, 3, 3, '个', 25000, 0.08, 2000, 1, 1001, '0'),
(4, 2, 4, 4, '个', 50000, 0.02, 1000, 1, 1001, '0'),
(5, 3, 7, 7, '个', 1000, 2.50, 2500, 1, 1001, '0'),
(6, 3, 8, 8, '米', 5000, 0.30, 1500, 1, 1001, '0'),
(7, 3, 9, 9, '卷', 100, 3.00, 300, 1, 1001, '0'),
(8, 4, 10, 10, '个', 5000, 0.10, 500, 1, 1001, '0'),
(9, 4, 6, 6, '个', 10000, 0.03, 300, 2, 1001, '0'),
(10, 5, 5, 5, '个', 100000, 0.01, 1000, 1, 1001, '0'),
(11, 5, 4, 4, '个', 50000, 0.02, 1000, 1, 1001, '0'),
(12, 6, 1, 1, '个', 50000, 0.01, 500, 1, 1001, '0'),
(13, 6, 3, 3, '个', 10000, 0.07, 700, 1, 1001, '0'),
(14, 7, 2, 2, '个', 50000, 0.05, 2500, 1, 1001, '0'),
(15, 7, 3, 3, '个', 50000, 0.07, 3500, 1, 1001, '0'),
(16, 8, 8, 8, '米', 1000, 0.30, 300, 1, 1001, '0'),
(17, 8, 9, 9, '卷', 50, 3.00, 150, 1, 1001, '0'),
(18, 9, 10, 10, '个', 5000, 0.10, 500, 1, 1001, '0'),
(19, 9, 6, 6, '个', 10000, 0.04, 400, 2, 1001, '0'),
(20, 10, 7, 7, '个', 500, 2.50, 1250, 1, 1001, '0'),
(21, 10, 5, 5, '个', 100000, 0.01, 1000, 1, 1001, '0'),
(22, 10, 4, 4, '个', 50000, 0.025, 1250, 1, 1001, '0')
ON CONFLICT (id) DO UPDATE SET header_id = EXCLUDED.header_id, material_id = EXCLUDED.material_id, oper_number = EXCLUDED.oper_number, unit_price = EXCLUDED.unit_price, all_price = EXCLUDED.all_price, depot_id = EXCLUDED.depot_id, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;

-- ============================================================
-- 18. 销售出库单 (jsh_depot_head + jsh_depot_item) - A001 的 10 笔销售
-- ============================================================
INSERT INTO jsh_depot_head (id, type, sub_type, number, create_time, oper_time, organ_id, creator, account_id, change_amount, total_price, pay_type, status, sales_man, tenant_id, delete_flag) VALUES
(11, '出库', '销售', 'SALE-2025-0001', '2025-01-20 10:00:00', '2025-01-20 10:00:00', 6, 203, 1, 2400, 2400, '现付', '0', '王五', 1001, '0'),
(12, '出库', '销售', 'SALE-2025-0002', '2025-02-15 09:00:00', '2025-02-15 09:00:00', 7, 203, 1, 3600, 3600, '月结', '0', '王五', 1001, '0'),
(13, '出库', '销售', 'SALE-2025-0003', '2025-03-10 14:00:00', '2025-03-10 14:00:00', 8, 207, 1, 1500, 1500, '现付', '0', '吴九', 1001, '0'),
(14, '出库', '销售', 'SALE-2025-0004', '2025-04-05 11:00:00', '2025-04-05 11:00:00', 9, 203, 2, 4800, 4800, '月结', '0', '王五', 1001, '0'),
(15, '出库', '销售', 'SALE-2025-0005', '2025-05-12 15:00:00', '2025-05-12 15:00:00', 10, 207, 1, 900, 900, '现付', '0', '吴九', 1001, '0'),
(16, '出库', '销售', 'SALE-2025-0006', '2025-06-08 08:30:00', '2025-06-08 08:30:00', 6, 203, 1, 5000, 5000, '月结', '0', '王五', 1001, '0'),
(17, '出库', '销售', 'SALE-2025-0007', '2025-06-25 10:00:00', '2025-06-25 10:00:00', 7, 207, 1, 1800, 1800, '现付', '0', '吴九', 1001, '0'),
(18, '出库', '销售', 'SALE-2025-0008', '2025-07-05 14:30:00', '2025-07-05 14:30:00', 8, 203, 2, 720, 720, '现付', '0', '王五', 1001, '0'),
(19, '出库', '销售', 'SALE-2025-0009', '2025-07-12 09:00:00', '2025-07-12 09:00:00', 9, 207, 1, 3000, 3000, '月结', '0', '吴九', 1001, '0'),
(20, '出库', '销售', 'SALE-2025-0010', '2025-07-18 16:00:00', '2025-07-18 16:00:00', 10, 203, 1, 1200, 1200, '现付', '0', '王五', 1001, '0')
ON CONFLICT (id) DO UPDATE SET type = EXCLUDED.type, sub_type = EXCLUDED.sub_type, number = EXCLUDED.number, organ_id = EXCLUDED.organ_id, creator = EXCLUDED.creator, total_price = EXCLUDED.total_price, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;

INSERT INTO jsh_depot_item (id, header_id, material_id, material_extend_id, material_unit, oper_number, unit_price, all_price, depot_id, tenant_id, delete_flag) VALUES
(23, 11, 1, 1, '个', 50000, 0.02, 1000, 1, 1001, '0'),
(24, 11, 3, 3, '个', 10000, 0.15, 1500, 1, 1001, '0'),
(25, 12, 7, 7, '个', 500, 4.00, 2000, 1, 1001, '0'),
(26, 12, 8, 8, '米', 2000, 0.60, 1200, 1, 1001, '0'),
(27, 12, 9, 9, '卷', 80, 5.00, 400, 1, 1001, '0'),
(28, 13, 4, 4, '个', 30000, 0.05, 1500, 1, 1001, '0'),
(29, 14, 2, 2, '个', 20000, 0.10, 2000, 1, 1001, '0'),
(30, 14, 3, 3, '个', 20000, 0.14, 2800, 1, 1001, '0'),
(31, 15, 10, 10, '个', 5000, 0.18, 900, 1, 1001, '0'),
(32, 16, 5, 5, '个', 100000, 0.03, 3000, 1, 1001, '0'),
(33, 16, 6, 6, '个', 40000, 0.05, 2000, 1, 1001, '0'),
(34, 17, 1, 1, '个', 50000, 0.02, 1000, 1, 1001, '0'),
(35, 17, 4, 4, '个', 20000, 0.04, 800, 1, 1001, '0'),
(36, 18, 9, 9, '卷', 60, 5.00, 300, 1, 1001, '0'),
(37, 18, 10, 10, '个', 2000, 0.18, 360, 1, 1001, '0'),
(38, 18, 8, 8, '米', 200, 0.30, 60, 1, 1001, '0'),
(39, 19, 7, 7, '个', 400, 4.00, 1600, 1, 1001, '0'),
(40, 19, 3, 3, '个', 10000, 0.14, 1400, 1, 1001, '0'),
(41, 20, 2, 2, '个', 5000, 0.10, 500, 1, 1001, '0'),
(42, 20, 6, 6, '个', 10000, 0.05, 500, 2, 1001, '0'),
(43, 20, 9, 9, '卷', 40, 5.00, 200, 1, 1001, '0')
ON CONFLICT (id) DO UPDATE SET header_id = EXCLUDED.header_id, material_id = EXCLUDED.material_id, oper_number = EXCLUDED.oper_number, unit_price = EXCLUDED.unit_price, all_price = EXCLUDED.all_price, depot_id = EXCLUDED.depot_id, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;

-- ============================================================
-- 19. 收入/支出单 (jsh_account_head + jsh_account_item) - A001 的 10 笔财务
-- ============================================================
INSERT INTO jsh_account_head (id, type, organ_id, creator, change_amount, total_price, account_id, bill_no, bill_time, status, tenant_id, delete_flag) VALUES
(1, '收入', 6, 205, 2400, 2400, 1, 'FIN-IN-0001', '2025-01-25 10:00:00', '0', 1001, '0'),
(2, '收入', 8, 205, 1500, 1500, 1, 'FIN-IN-0002', '2025-03-15 14:00:00', '0', 1001, '0'),
(3, '收入', 10, 209, 900, 900, 2, 'FIN-IN-0003', '2025-05-20 09:00:00', '0', 1001, '0'),
(4, '支出', 1, 205, 1500, 1500, 1, 'FIN-OUT-0001', '2025-02-01 10:00:00', '0', 1001, '0'),
(5, '支出', 3, 205, 5000, 5000, 1, 'FIN-OUT-0002', '2025-03-10 09:00:00', '0', 1001, '0'),
(6, '支出', 2, 209, 3000, 3000, 1, 'FIN-OUT-0003', '2025-05-01 14:00:00', '0', 1001, '0'),
(7, '收入', NULL, 205, 500, 500, 2, 'FIN-IN-0004', '2025-04-01 10:00:00', '0', 1001, '0'),
(8, '支出', NULL, 209, 300, 300, 2, 'FIN-OUT-0004', '2025-04-15 09:00:00', '0', 1001, '0'),
(9, '收入', 9, 205, 4800, 4800, 1, 'FIN-IN-0005', '2025-06-15 14:00:00', '0', 1001, '0'),
(10, '支出', 4, 209, 800, 800, 1, 'FIN-OUT-0005', '2025-07-01 10:00:00', '0', 1001, '0')
ON CONFLICT (id) DO UPDATE SET type = EXCLUDED.type, organ_id = EXCLUDED.organ_id, account_id = EXCLUDED.account_id, total_price = EXCLUDED.total_price, bill_no = EXCLUDED.bill_no, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;

INSERT INTO jsh_account_item (id, account_id, in_out_item_id, each_amount, remark, header_id, tenant_id, delete_flag) VALUES
(1, 1, NULL, 2400, '北京科技货款', 1, 1001, '0'),
(2, 1, NULL, 1500, '杭州电商货款', 2, 1001, '0'),
(3, 2, NULL, 900, '武汉零售货款', 3, 1001, '0'),
(4, 1, 1, 1500, '快递费', 4, 1001, '0'),
(5, 1, NULL, 5000, '华强电子货款', 5, 1001, '0'),
(6, 1, NULL, 3000, '佛山塑料货款', 6, 1001, '0'),
(7, 2, 2, 500, '房租收入', 7, 1001, '0'),
(8, 2, 5, 300, '办公用品', 8, 1001, '0'),
(9, 1, NULL, 4800, '成都批发货款', 9, 1001, '0'),
(10, 1, 4, 800, '水电费', 10, 1001, '0')
ON CONFLICT (id) DO UPDATE SET account_id = EXCLUDED.account_id, each_amount = EXCLUDED.each_amount, remark = EXCLUDED.remark, header_id = EXCLUDED.header_id, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;

-- ============================================================
-- 20. B001 普贸达 采购入库单 (10笔)
-- ============================================================
INSERT INTO jsh_depot_head (id, type, sub_type, number, create_time, oper_time, organ_id, creator, account_id, change_amount, total_price, pay_type, status, sales_man, tenant_id, delete_flag) VALUES
(101, '入库', '采购', 'PUR-2025-0001', '2025-01-10 10:00:00', '2025-01-10 10:00:00', 11, 302, 4, 5000, 5000, '现付', '0', '黄经理', 1002, '0'),
(102, '入库', '采购', 'PUR-2025-0002', '2025-02-08 09:00:00', '2025-02-08 09:00:00', 12, 305, 4, 3000, 3000, '现付', '0', '罗采购', 1002, '0'),
(103, '入库', '采购', 'PUR-2025-0003', '2025-03-15 14:00:00', '2025-03-15 14:00:00', 13, 302, 4, 8000, 8000, '月结', '0', '黄经理', 1002, '0'),
(104, '入库', '采购', 'PUR-2025-0004', '2025-04-12 11:00:00', '2025-04-12 11:00:00', 14, 305, 4, 2500, 2500, '现付', '0', '罗采购', 1002, '0'),
(105, '入库', '采购', 'PUR-2025-0005', '2025-05-20 15:00:00', '2025-05-20 15:00:00', 15, 302, 4, 6000, 6000, '月结', '0', '黄经理', 1002, '0'),
(106, '入库', '采购', 'PUR-2025-0006', '2025-06-05 08:00:00', '2025-06-05 08:00:00', 11, 305, 4, 4000, 4000, '现付', '0', '罗采购', 1002, '0'),
(107, '入库', '采购', 'PUR-2025-0007', '2025-06-20 10:00:00', '2025-06-20 10:00:00', 12, 302, 4, 1500, 1500, '现付', '0', '黄经理', 1002, '0'),
(108, '入库', '采购', 'PUR-2025-0008', '2025-07-01 09:00:00', '2025-07-01 09:00:00', 13, 305, 4, 7000, 7000, '月结', '0', '罗采购', 1002, '0'),
(109, '入库', '采购', 'PUR-2025-0009', '2025-07-10 14:00:00', '2025-07-10 14:00:00', 14, 302, 4, 1800, 1800, '现付', '0', '黄经理', 1002, '0'),
(110, '入库', '采购', 'PUR-2025-0010', '2025-07-18 16:00:00', '2025-07-18 16:00:00', 15, 305, 4, 3500, 3500, '月结', '0', '罗采购', 1002, '0')
ON CONFLICT (id) DO UPDATE SET type = EXCLUDED.type, sub_type = EXCLUDED.sub_type, number = EXCLUDED.number, organ_id = EXCLUDED.organ_id, creator = EXCLUDED.creator, total_price = EXCLUDED.total_price, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;

INSERT INTO jsh_depot_item (id, header_id, material_id, material_extend_id, material_unit, oper_number, unit_price, all_price, depot_id, tenant_id, delete_flag) VALUES
(101, 101, 11, 11, '个', 200, 15.00, 3000, 4, 1002, '0'),
(102, 101, 12, 12, '把', 200, 8.00, 1600, 4, 1002, '0'),
(103, 101, 13, 13, '个', 200, 2.00, 400, 4, 1002, '0'),
(104, 102, 14, 14, '件', 100, 12.00, 1200, 4, 1002, '0'),
(105, 102, 15, 15, '条', 50, 25.00, 1250, 4, 1002, '0'),
(106, 102, 16, 16, '顶', 100, 5.00, 500, 4, 1002, '0'),
(107, 103, 17, 17, '个', 200, 8.00, 1600, 4, 1002, '0'),
(108, 103, 18, 18, '盒', 100, 15.00, 1500, 4, 1002, '0'),
(109, 103, 19, 19, '个', 100, 25.00, 2500, 4, 1002, '0'),
(110, 103, 20, 20, '套', 150, 10.00, 1500, 4, 1002, '0'),
(111, 104, 14, 14, '件', 100, 12.00, 1200, 4, 1002, '0'),
(112, 104, 15, 15, '条', 40, 25.00, 1000, 4, 1002, '0'),
(113, 104, 16, 16, '顶', 60, 5.00, 300, 4, 1002, '0'),
(114, 105, 11, 11, '个', 200, 15.00, 3000, 4, 1002, '0'),
(115, 105, 13, 13, '个', 500, 2.00, 1000, 4, 1002, '0'),
(116, 105, 12, 12, '把', 200, 8.00, 1600, 4, 1002, '0'),
(117, 106, 17, 17, '个', 100, 8.00, 800, 4, 1002, '0'),
(118, 106, 18, 18, '盒', 80, 15.00, 1200, 4, 1002, '0'),
(119, 106, 20, 20, '套', 100, 10.00, 1000, 4, 1002, '0'),
(120, 106, 19, 19, '个', 40, 25.00, 1000, 4, 1002, '0'),
(121, 107, 16, 16, '顶', 200, 5.00, 1000, 4, 1002, '0'),
(122, 107, 13, 13, '个', 250, 2.00, 500, 4, 1002, '0'),
(123, 108, 11, 11, '个', 200, 15.00, 3000, 4, 1002, '0'),
(124, 108, 12, 12, '把', 200, 8.00, 1600, 4, 1002, '0'),
(125, 108, 14, 14, '件', 100, 12.00, 1200, 4, 1002, '0'),
(126, 108, 15, 15, '条', 50, 24.00, 1200, 4, 1002, '0'),
(127, 109, 19, 19, '个', 40, 25.00, 1000, 4, 1002, '0'),
(128, 109, 20, 20, '套', 80, 10.00, 800, 4, 1002, '0'),
(129, 110, 17, 17, '个', 100, 8.00, 800, 4, 1002, '0'),
(130, 110, 18, 18, '盒', 80, 15.00, 1200, 4, 1002, '0'),
(131, 110, 13, 13, '个', 500, 2.00, 1000, 4, 1002, '0'),
(132, 110, 16, 16, '顶', 100, 5.00, 500, 4, 1002, '0')
ON CONFLICT (id) DO UPDATE SET header_id = EXCLUDED.header_id, material_id = EXCLUDED.material_id, oper_number = EXCLUDED.oper_number, unit_price = EXCLUDED.unit_price, all_price = EXCLUDED.all_price, depot_id = EXCLUDED.depot_id, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;

-- ============================================================
-- 21. B001 普贸达 销售出库单 (10笔)
-- ============================================================
INSERT INTO jsh_depot_head (id, type, sub_type, number, create_time, oper_time, organ_id, creator, account_id, change_amount, total_price, pay_type, status, sales_man, tenant_id, delete_flag) VALUES
(111, '出库', '销售', 'SALE-2025-0001', '2025-01-18 10:00:00', '2025-01-18 10:00:00', 16, 303, 4, 8000, 8000, '现付', '0', '林经理', 1002, '0'),
(112, '出库', '销售', 'SALE-2025-0002', '2025-02-20 09:00:00', '2025-02-20 09:00:00', 17, 306, 4, 5000, 5000, '月结', '0', '梁销售', 1002, '0'),
(113, '出库', '销售', 'SALE-2025-0003', '2025-03-25 14:00:00', '2025-03-25 14:00:00', 18, 303, 4, 12000, 12000, '月结', '0', '林经理', 1002, '0'),
(114, '出库', '销售', 'SALE-2025-0004', '2025-04-15 11:00:00', '2025-04-15 11:00:00', 19, 306, 4, 6000, 6000, '现付', '0', '梁销售', 1002, '0'),
(115, '出库', '销售', 'SALE-2025-0005', '2025-05-10 15:00:00', '2025-05-10 15:00:00', 20, 303, 4, 4500, 4500, '现付', '0', '林经理', 1002, '0'),
(116, '出库', '销售', 'SALE-2025-0006', '2025-06-01 08:00:00', '2025-06-01 08:00:00', 16, 306, 4, 9000, 9000, '月结', '0', '梁销售', 1002, '0'),
(117, '出库', '销售', 'SALE-2025-0007', '2025-06-18 10:00:00', '2025-06-18 10:00:00', 17, 303, 4, 3500, 3500, '现付', '0', '林经理', 1002, '0'),
(118, '出库', '销售', 'SALE-2025-0008', '2025-07-05 14:00:00', '2025-07-05 14:00:00', 18, 306, 4, 7500, 7500, '月结', '0', '梁销售', 1002, '0'),
(119, '出库', '销售', 'SALE-2025-0009', '2025-07-12 09:00:00', '2025-07-12 09:00:00', 19, 303, 4, 2800, 2800, '现付', '0', '林经理', 1002, '0'),
(120, '出库', '销售', 'SALE-2025-0010', '2025-07-20 16:00:00', '2025-07-20 16:00:00', 20, 306, 4, 5500, 5500, '月结', '0', '梁销售', 1002, '0')
ON CONFLICT (id) DO UPDATE SET type = EXCLUDED.type, sub_type = EXCLUDED.sub_type, number = EXCLUDED.number, organ_id = EXCLUDED.organ_id, creator = EXCLUDED.creator, total_price = EXCLUDED.total_price, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;

INSERT INTO jsh_depot_item (id, header_id, material_id, material_extend_id, material_unit, oper_number, unit_price, all_price, depot_id, tenant_id, delete_flag) VALUES
(133, 111, 11, 11, '个', 200, 35.00, 7000, 4, 1002, '0'),
(134, 111, 13, 13, '个', 200, 5.00, 1000, 4, 1002, '0'),
(135, 112, 14, 14, '件', 100, 35.00, 3500, 4, 1002, '0'),
(136, 112, 16, 16, '顶', 200, 7.50, 1500, 4, 1002, '0'),
(137, 113, 17, 17, '个', 200, 25.00, 5000, 4, 1002, '0'),
(138, 113, 18, 18, '盒', 100, 38.00, 3800, 4, 1002, '0'),
(139, 113, 19, 19, '个', 80, 40.00, 3200, 4, 1002, '0'),
(140, 114, 15, 15, '条', 60, 65.00, 3900, 4, 1002, '0'),
(141, 114, 12, 12, '把', 100, 18.00, 1800, 4, 1002, '0'),
(142, 114, 20, 20, '套', 30, 10.00, 300, 4, 1002, '0'),
(143, 115, 11, 11, '个', 100, 35.00, 3500, 4, 1002, '0'),
(144, 115, 13, 13, '个', 500, 2.00, 1000, 4, 1002, '0'),
(145, 116, 14, 14, '件', 150, 35.00, 5250, 4, 1002, '0'),
(146, 116, 15, 15, '条', 40, 65.00, 2600, 4, 1002, '0'),
(147, 116, 16, 16, '顶', 100, 11.50, 1150, 4, 1002, '0'),
(148, 117, 12, 12, '把', 200, 15.00, 3000, 4, 1002, '0'),
(149, 117, 13, 13, '个', 250, 2.00, 500, 4, 1002, '0'),
(150, 118, 18, 18, '盒', 100, 38.00, 3800, 4, 1002, '0'),
(151, 118, 19, 19, '个', 80, 35.00, 2800, 4, 1002, '0'),
(152, 118, 20, 20, '套', 90, 10.00, 900, 4, 1002, '0'),
(153, 119, 17, 17, '个', 100, 18.00, 1800, 4, 1002, '0'),
(154, 119, 16, 16, '顶', 200, 5.00, 1000, 4, 1002, '0'),
(155, 120, 11, 11, '个', 100, 35.00, 3500, 4, 1002, '0'),
(156, 120, 14, 14, '件', 50, 30.00, 1500, 4, 1002, '0'),
(157, 120, 12, 12, '把', 50, 10.00, 500, 4, 1002, '0')
ON CONFLICT (id) DO UPDATE SET header_id = EXCLUDED.header_id, material_id = EXCLUDED.material_id, oper_number = EXCLUDED.oper_number, unit_price = EXCLUDED.unit_price, all_price = EXCLUDED.all_price, depot_id = EXCLUDED.depot_id, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;

-- ============================================================
-- 22. 更新序列值（最终）
-- ============================================================
SELECT setval('jsh_depot_head_id_seq', GREATEST((SELECT MAX(id) FROM jsh_depot_head), 1));
SELECT setval('jsh_depot_item_id_seq', GREATEST((SELECT MAX(id) FROM jsh_depot_item), 1));
SELECT setval('jsh_depot_head_id_seq', GREATEST((SELECT MAX(id) FROM jsh_depot_head), 1));
SELECT setval('jsh_depot_item_id_seq', GREATEST((SELECT MAX(id) FROM jsh_depot_item), 1));
SELECT setval('jsh_account_head_id_seq', GREATEST((SELECT MAX(id) FROM jsh_account_head), 1));
SELECT setval('jsh_account_item_id_seq', GREATEST((SELECT MAX(id) FROM jsh_account_item), 1));

-- ============================================================
-- 完成
-- ============================================================
ANALYZE;
