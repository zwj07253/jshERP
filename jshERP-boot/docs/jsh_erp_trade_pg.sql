-- 外贸手表 Demo：PostgreSQL 初始化脚本
-- 依赖 jsh_erp_pg.sql 与 watch_test_data.sql 已导入；脚本可重复执行。

CREATE TABLE IF NOT EXISTS jsh_trade_shipment (
    id BIGINT PRIMARY KEY,
    shipment_no VARCHAR(50) NOT NULL,
    container_no VARCHAR(100),
    bill_of_lading_no VARCHAR(100),
    origin_port VARCHAR(100) NOT NULL,
    destination_port VARCHAR(100) NOT NULL,
    carrier_name VARCHAR(100),
    incoterms VARCHAR(20) DEFAULT 'CIF',
    currency VARCHAR(10) DEFAULT 'USD',
    exchange_rate NUMERIC(24,6) DEFAULT 7.200000,
    departure_date TIMESTAMP,
    estimated_arrival_date TIMESTAMP,
    actual_arrival_date TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL,
    delete_flag VARCHAR(1) DEFAULT '0'
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_trade_shipment_tenant_no
    ON jsh_trade_shipment(tenant_id, shipment_no);
CREATE INDEX IF NOT EXISTS ix_trade_shipment_tenant_status
    ON jsh_trade_shipment(tenant_id, status);

CREATE TABLE IF NOT EXISTS jsh_trade_shipment_item (
    id BIGINT PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    depot_head_id BIGINT,
    depot_item_id BIGINT,
    sales_depot_head_id BIGINT,
    sales_depot_item_id BIGINT,
    quantity NUMERIC(24,6) NOT NULL DEFAULT 0,
    purchase_amount NUMERIC(24,6) NOT NULL DEFAULT 0,
    sales_amount NUMERIC(24,6) NOT NULL DEFAULT 0,
    in_transit_quantity NUMERIC(24,6) NOT NULL DEFAULT 0,
    cleared_quantity NUMERIC(24,6) NOT NULL DEFAULT 0,
    stocked_quantity NUMERIC(24,6) NOT NULL DEFAULT 0,
    sold_quantity NUMERIC(24,6) NOT NULL DEFAULT 0,
    tenant_id BIGINT NOT NULL,
    delete_flag VARCHAR(1) DEFAULT '0'
);

CREATE INDEX IF NOT EXISTS ix_trade_shipment_item_shipment
    ON jsh_trade_shipment_item(shipment_id);
CREATE INDEX IF NOT EXISTS ix_trade_shipment_item_material
    ON jsh_trade_shipment_item(material_id);

CREATE TABLE IF NOT EXISTS jsh_trade_document (
    id BIGINT PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    document_type VARCHAR(80) NOT NULL,
    document_no VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    owner_name VARCHAR(50),
    due_date TIMESTAMP,
    attachment_name VARCHAR(255),
    exception_note VARCHAR(500),
    tenant_id BIGINT NOT NULL,
    delete_flag VARCHAR(1) DEFAULT '0'
);

CREATE INDEX IF NOT EXISTS ix_trade_document_shipment
    ON jsh_trade_document(shipment_id);
CREATE INDEX IF NOT EXISTS ix_trade_document_status
    ON jsh_trade_document(tenant_id, status);

CREATE TABLE IF NOT EXISTS jsh_trade_cost (
    id BIGINT PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    cost_type VARCHAR(50) NOT NULL,
    original_amount NUMERIC(24,6) NOT NULL DEFAULT 0,
    currency VARCHAR(10) NOT NULL DEFAULT 'CNY',
    exchange_rate NUMERIC(24,6) NOT NULL DEFAULT 1,
    cny_amount NUMERIC(24,6) NOT NULL DEFAULT 0,
    allocation_method VARCHAR(30) DEFAULT 'PURCHASE_AMOUNT',
    allocated_flag VARCHAR(1) DEFAULT '0',
    cost_date TIMESTAMP,
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL,
    delete_flag VARCHAR(1) DEFAULT '0'
);

CREATE INDEX IF NOT EXISTS ix_trade_cost_shipment
    ON jsh_trade_cost(shipment_id);

CREATE TABLE IF NOT EXISTS jsh_trade_material_cost (
    id BIGINT PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    purchase_cost NUMERIC(24,6) NOT NULL DEFAULT 0,
    logistics_cost NUMERIC(24,6) NOT NULL DEFAULT 0,
    duty_cost NUMERIC(24,6) NOT NULL DEFAULT 0,
    local_cost NUMERIC(24,6) NOT NULL DEFAULT 0,
    landed_cost NUMERIC(24,6) NOT NULL DEFAULT 0,
    calculated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL,
    delete_flag VARCHAR(1) DEFAULT '0'
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_trade_material_cost_shipment_material
    ON jsh_trade_material_cost(shipment_id, material_id);

-- 外贸管理菜单。使用 300~305，避免与现有功能 ID 冲突。
INSERT INTO jsh_function (id, number, name, parent_number, url, component, state, sort, enabled, type, push_btn, icon, delete_flag) VALUES
(300, '0700', '外贸管理', '0', '/trade', '/layouts/TabLayout', FALSE, '0800', TRUE, '电脑版', '', 'global', '0'),
(301, '070001', '外贸看板', '0700', '/trade/dashboard', '/trade/TradeDashboard', FALSE, '0801', TRUE, '电脑版', '', 'dashboard', '0'),
(302, '070002', '发运批次', '0700', '/trade/shipment', '/trade/TradeShipmentList', FALSE, '0802', TRUE, '电脑版', '1,2,3', 'car', '0'),
(303, '070003', '清关单证', '0700', '/trade/document', '/trade/TradeDocumentList', FALSE, '0803', TRUE, '电脑版', '1,2,3', 'file-done', '0'),
(304, '070004', '外贸库存', '0700', '/trade/inventory', '/trade/TradeInventory', FALSE, '0804', TRUE, '电脑版', '', 'database', '0'),
(305, '070005', '进口成本', '0700', '/trade/cost', '/trade/TradeCostReport', FALSE, '0805', TRUE, '电脑版', '', 'money-collect', '0')
ON CONFLICT (id) DO UPDATE SET
number = EXCLUDED.number, name = EXCLUDED.name, parent_number = EXCLUDED.parent_number,
url = EXCLUDED.url, component = EXCLUDED.component, state = EXCLUDED.state, sort = EXCLUDED.sort,
enabled = EXCLUDED.enabled, type = EXCLUDED.type, push_btn = EXCLUDED.push_btn, icon = EXCLUDED.icon,
delete_flag = EXCLUDED.delete_flag;

UPDATE jsh_user_business
SET value = value || '[300][301][302][303][304][305]'
WHERE tenant_id = 100 AND type = 'RoleFunctions' AND value NOT LIKE '%[300]%';

UPDATE jsh_user_business
SET value = value || '[300][301][302][303][304][305]'
WHERE tenant_id = 0 AND type = 'RoleFunctions' AND key_id = '4' AND value NOT LIKE '%[300]%';

-- 修复早期脚本直接拼接 JSON 数组导致的 `][`，否则按钮权限接口会解析失败。
UPDATE jsh_user_business
SET btn_str = REPLACE(btn_str, '][', ',')
WHERE tenant_id = 100 AND type = 'RoleFunctions' AND btn_str LIKE '%][%';

UPDATE jsh_user_business
SET btn_str = CASE
    WHEN COALESCE(TRIM(btn_str), '') = '' THEN '[{"funId":302,"btnStr":"1,2,3"},{"funId":303,"btnStr":"1,2,3"}]'
    ELSE LEFT(TRIM(btn_str), LENGTH(TRIM(btn_str)) - 1)
         || ',{"funId":302,"btnStr":"1,2,3"},{"funId":303,"btnStr":"1,2,3"}]'
END
WHERE tenant_id = 100 AND type = 'RoleFunctions' AND COALESCE(btn_str, '') NOT LIKE '%"funId":302%';

UPDATE jsh_user_business
SET btn_str = CASE
    WHEN COALESCE(TRIM(btn_str), '') = '' THEN '[{"funId":302,"btnStr":"1,2,3"},{"funId":303,"btnStr":"1,2,3"}]'
    ELSE LEFT(TRIM(btn_str), LENGTH(TRIM(btn_str)) - 1)
         || ',{"funId":302,"btnStr":"1,2,3"},{"funId":303,"btnStr":"1,2,3"}]'
END
WHERE tenant_id = 0 AND type = 'RoleFunctions' AND key_id = '4' AND COALESCE(btn_str, '') NOT LIKE '%"funId":302%';

-- 演示账号 zhangwei 的岗位为采购经理，角色名称保持一致，避免权限配置产生误导。
UPDATE jsh_role
SET name = '采购经理', description = '采购、请购与供应商相关权限'
WHERE id = 102 AND tenant_id = 100;

-- 墨西哥演示客户。
INSERT INTO jsh_supplier (id, supplier, contacts, phone_num, email, description, isystem, type, enabled,
advance_in, begin_need_get, begin_need_pay, all_need_get, all_need_pay, fax, telephone, address, tax_num,
bank_name, account_number, tax_rate, sort, creator, tenant_id, delete_flag)
VALUES
(109, 'México Tiempo Distribución', 'Carlos Hernández', '+52-55-5555-0101', 'carlos@tiempo.mx',
 '墨西哥钟表分销商，Demo 用客户', 1, '客户', TRUE, 0, 80000, 0, 80000, 0, NULL, '+52-55-5555-0101',
 'Ciudad de México, México', 'MTD010101ABC', NULL, NULL, 0, '9', 101, 100, '0')
ON CONFLICT (id) DO NOTHING;

-- 三条外贸发运批次：已入库、清关中、海运在途。
INSERT INTO jsh_trade_shipment (id, shipment_no, container_no, bill_of_lading_no, origin_port, destination_port,
carrier_name, incoterms, currency, exchange_rate, departure_date, estimated_arrival_date, actual_arrival_date,
status, remark, tenant_id, delete_flag) VALUES
(1001, 'SH-MX-2026-001', 'MSKU-2026001', 'BL-SZX-MZN-001', '深圳盐田港', 'Manzanillo', 'Maersk', 'CIF', 'USD', 7.200000,
 '2026-05-15 10:00:00', '2026-06-12 10:00:00', '2026-06-11 16:00:00', '已入库', '瑞士机械表首批进口，已完成墨西哥入库', 100, '0'),
(1002, 'SH-MX-2026-002', 'MSKU-2026002', 'BL-SZX-MZN-002', '深圳盐田港', 'Manzanillo', 'COSCO', 'CIF', 'USD', 7.200000,
 '2026-06-18 10:00:00', '2026-07-15 10:00:00', '2026-07-14 09:00:00', '清关中', '日本精工系列到港，等待 Pedimento 放行', 100, '0'),
(1003, 'SH-MX-2026-003', 'MSKU-2026003', 'BL-SZX-MZN-003', '深圳盐田港', 'Manzanillo', 'Maersk', 'FOB', 'USD', 7.200000,
 '2026-07-08 10:00:00', '2026-08-05 10:00:00', NULL, '在途', '智能手表与配件海运在途', 100, '0')
ON CONFLICT (id) DO UPDATE SET
shipment_no = EXCLUDED.shipment_no, container_no = EXCLUDED.container_no, bill_of_lading_no = EXCLUDED.bill_of_lading_no,
origin_port = EXCLUDED.origin_port, destination_port = EXCLUDED.destination_port, carrier_name = EXCLUDED.carrier_name,
incoterms = EXCLUDED.incoterms, currency = EXCLUDED.currency, exchange_rate = EXCLUDED.exchange_rate,
departure_date = EXCLUDED.departure_date, estimated_arrival_date = EXCLUDED.estimated_arrival_date,
actual_arrival_date = EXCLUDED.actual_arrival_date, status = EXCLUDED.status, remark = EXCLUDED.remark,
tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;

INSERT INTO jsh_trade_shipment_item (id, shipment_id, material_id, depot_head_id, quantity, purchase_amount,
in_transit_quantity, cleared_quantity, stocked_quantity, sold_quantity, tenant_id, delete_flag) VALUES
(1101, 1001, 101, 101, 10, 120000, 0, 10, 10, 2, 100, '0'),
(1102, 1001, 102, 101, 8, 72000, 0, 8, 8, 2, 100, '0'),
(1103, 1001, 103, 101, 20, 58000, 0, 20, 20, 5, 100, '0'),
(1201, 1002, 104, 102, 20, 85000, 0, 20, 0, 0, 100, '0'),
(1202, 1002, 105, 102, 12, 66000, 0, 12, 0, 0, 100, '0'),
(1301, 1003, 109, 104, 15, 90000, 15, 0, 0, 0, 100, '0'),
(1302, 1003, 110, 104, 20, 60000, 20, 0, 0, 0, 100, '0'),
(1303, 1003, 111, 104, 8, 48000, 8, 0, 0, 0, 100, '0')
ON CONFLICT (id) DO UPDATE SET
shipment_id = EXCLUDED.shipment_id, material_id = EXCLUDED.material_id, depot_head_id = EXCLUDED.depot_head_id,
quantity = EXCLUDED.quantity, purchase_amount = EXCLUDED.purchase_amount, in_transit_quantity = EXCLUDED.in_transit_quantity,
cleared_quantity = EXCLUDED.cleared_quantity, stocked_quantity = EXCLUDED.stocked_quantity, sold_quantity = EXCLUDED.sold_quantity,
tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;

-- 墨西哥分销销售单，用于演示采购批次到海外客户销售的闭环。
INSERT INTO jsh_depot_head (id, type, sub_type, default_number, number, create_time, oper_time, organ_id, creator,
account_id, change_amount, back_amount, total_price, pay_type, bill_type, remark, sales_man, discount, discount_money,
discount_last_money, other_money, deposit, debt, last_debt, status, purchase_status, source, tenant_id, delete_flag) VALUES
(510, '出库', '销售', 'MX-SO-20260712-001', 'MX-SO-20260712-001', '2026-07-12 10:00:00', '2026-07-12 10:20:00',
109, 103, 101, 85600, 0, 85600, '记账', '销售', '墨西哥分销客户订单，关联外贸发运批次 SH-MX-2026-001', '李娜',
0, 0, 85600, 0, 0, 85600, 0, '1', '2', '0', 100, '0')
ON CONFLICT (id) DO UPDATE SET
type = EXCLUDED.type, sub_type = EXCLUDED.sub_type, default_number = EXCLUDED.default_number, number = EXCLUDED.number,
create_time = EXCLUDED.create_time, oper_time = EXCLUDED.oper_time, organ_id = EXCLUDED.organ_id, creator = EXCLUDED.creator,
account_id = EXCLUDED.account_id, change_amount = EXCLUDED.change_amount, back_amount = EXCLUDED.back_amount,
total_price = EXCLUDED.total_price, pay_type = EXCLUDED.pay_type, bill_type = EXCLUDED.bill_type, remark = EXCLUDED.remark,
sales_man = EXCLUDED.sales_man, discount = EXCLUDED.discount, discount_money = EXCLUDED.discount_money,
discount_last_money = EXCLUDED.discount_last_money, other_money = EXCLUDED.other_money, deposit = EXCLUDED.deposit,
debt = EXCLUDED.debt, last_debt = EXCLUDED.last_debt, status = EXCLUDED.status, purchase_status = EXCLUDED.purchase_status,
source = EXCLUDED.source, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;

INSERT INTO jsh_depot_item (id, header_id, material_id, material_extend_id, material_unit, oper_number, basic_number,
unit_price, purchase_unit_price, all_price, remark, depot_id, tenant_id, delete_flag) VALUES
(5101, 510, 101, 101, '只', 2, 2, 42800, 12000, 85600, '墨西哥分销销售 天梭力洛克 x2', 101, 100, '0')
ON CONFLICT (id) DO UPDATE SET
header_id = EXCLUDED.header_id, material_id = EXCLUDED.material_id, material_extend_id = EXCLUDED.material_extend_id,
material_unit = EXCLUDED.material_unit, oper_number = EXCLUDED.oper_number, basic_number = EXCLUDED.basic_number,
unit_price = EXCLUDED.unit_price, purchase_unit_price = EXCLUDED.purchase_unit_price, all_price = EXCLUDED.all_price,
remark = EXCLUDED.remark, depot_id = EXCLUDED.depot_id, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;

INSERT INTO jsh_trade_document (id, shipment_id, document_type, document_no, status, owner_name, due_date,
attachment_name, exception_note, tenant_id, delete_flag) VALUES
(2001, 1001, 'Commercial Invoice', 'CI-MX-001', '已通过', '李娜', '2026-05-14 18:00:00', 'CI-MX-001.pdf', NULL, 100, '0'),
(2002, 1001, 'Packing List', 'PL-MX-001', '已通过', '张伟', '2026-05-14 18:00:00', 'PL-MX-001.pdf', NULL, 100, '0'),
(2003, 1001, 'Pedimento', 'PED-2026-001', '已通过', '墨西哥报关行', '2026-06-10 18:00:00', 'PED-2026-001.pdf', NULL, 100, '0'),
(2101, 1002, 'Commercial Invoice', 'CI-MX-002', '已通过', '李娜', '2026-06-17 18:00:00', 'CI-MX-002.pdf', NULL, 100, '0'),
(2102, 1002, 'Bill of Lading', 'BL-SZX-MZN-002', '已通过', '张伟', '2026-06-17 18:00:00', 'BL-SZX-MZN-002.pdf', NULL, 100, '0'),
(2103, 1002, 'Pedimento', NULL, '审核中', '墨西哥报关行', '2026-07-15 18:00:00', NULL, '等待海关放行回执', 100, '0'),
(2104, 1002, 'NOM 文件', NULL, '缺失', '李娜', '2026-07-14 18:00:00', NULL, '智能产品标签西语版本待补充', 100, '0'),
(2201, 1003, 'Commercial Invoice', 'CI-MX-003', '已准备', '李娜', '2026-07-07 18:00:00', 'CI-MX-003.pdf', NULL, 100, '0'),
(2202, 1003, 'Packing List', 'PL-MX-003', '已准备', '张伟', '2026-07-07 18:00:00', 'PL-MX-003.pdf', NULL, 100, '0'),
(2203, 1003, 'Pedimento', NULL, '待准备', '墨西哥报关行', '2026-08-03 18:00:00', NULL, NULL, 100, '0')
ON CONFLICT (id) DO UPDATE SET
shipment_id = EXCLUDED.shipment_id, document_type = EXCLUDED.document_type, document_no = EXCLUDED.document_no,
status = EXCLUDED.status, owner_name = EXCLUDED.owner_name, due_date = EXCLUDED.due_date,
attachment_name = EXCLUDED.attachment_name, exception_note = EXCLUDED.exception_note, tenant_id = EXCLUDED.tenant_id,
delete_flag = EXCLUDED.delete_flag;

INSERT INTO jsh_trade_cost (id, shipment_id, cost_type, original_amount, currency, exchange_rate, cny_amount,
allocation_method, allocated_flag, cost_date, remark, tenant_id, delete_flag) VALUES
(3001, 1001, '海运费', 8500, 'USD', 7.2, 61200, 'PURCHASE_AMOUNT', '1', '2026-05-15 10:00:00', '深圳至 Manzanillo 海运费', 100, '0'),
(3002, 1001, '关税', 32000, 'CNY', 1, 32000, 'PURCHASE_AMOUNT', '1', '2026-06-11 10:00:00', '演示关税成本', 100, '0'),
(3003, 1001, '报关费', 5800, 'CNY', 1, 5800, 'PURCHASE_AMOUNT', '1', '2026-06-11 10:00:00', '报关代理服务费', 100, '0'),
(3101, 1002, '海运费', 6200, 'USD', 7.2, 44640, 'PURCHASE_AMOUNT', '0', '2026-06-18 10:00:00', '待清关批次预计海运费', 100, '0'),
(3102, 1002, '港口费', 12000, 'CNY', 1, 12000, 'PURCHASE_AMOUNT', '0', '2026-07-14 10:00:00', '港杂费预估', 100, '0'),
(3201, 1003, '海运费', 9800, 'USD', 7.2, 70560, 'PURCHASE_AMOUNT', '0', '2026-07-08 10:00:00', '智能手表海运费', 100, '0')
ON CONFLICT (id) DO UPDATE SET
shipment_id = EXCLUDED.shipment_id, cost_type = EXCLUDED.cost_type, original_amount = EXCLUDED.original_amount,
currency = EXCLUDED.currency, exchange_rate = EXCLUDED.exchange_rate, cny_amount = EXCLUDED.cny_amount,
allocation_method = EXCLUDED.allocation_method, allocated_flag = EXCLUDED.allocated_flag, cost_date = EXCLUDED.cost_date,
remark = EXCLUDED.remark, tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;

INSERT INTO jsh_trade_material_cost (id, shipment_id, material_id, purchase_cost, logistics_cost, duty_cost, local_cost,
landed_cost, calculated_time, tenant_id, delete_flag) VALUES
(4001, 1001, 101, 120000, 29376, 15360, 2784, 167520, '2026-06-12 18:00:00', 100, '0'),
(4002, 1001, 102, 72000, 17626, 9216, 1670, 100512, '2026-06-12 18:00:00', 100, '0'),
(4003, 1001, 103, 58000, 14198, 7424, 1346, 80968, '2026-06-12 18:00:00', 100, '0')
ON CONFLICT (id) DO UPDATE SET
purchase_cost = EXCLUDED.purchase_cost, logistics_cost = EXCLUDED.logistics_cost, duty_cost = EXCLUDED.duty_cost,
local_cost = EXCLUDED.local_cost, landed_cost = EXCLUDED.landed_cost, calculated_time = EXCLUDED.calculated_time,
tenant_id = EXCLUDED.tenant_id, delete_flag = EXCLUDED.delete_flag;
