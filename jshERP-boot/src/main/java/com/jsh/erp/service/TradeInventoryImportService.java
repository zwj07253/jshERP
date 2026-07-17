package com.jsh.erp.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** AI 库存解析结果的数据库匹配、业务校验和确认入库。 */
@Service
public class TradeInventoryImportService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> preview(List<Map<String, Object>> sourceRows, Long tenantId) {
        if (sourceRows == null || sourceRows.isEmpty()) {
            throw new IllegalArgumentException("AI 没有识别到库存明细");
        }
        if (sourceRows.size() > 1000) {
            throw new IllegalArgumentException("一次最多解析 1000 条库存明细");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> duplicateKeys = new HashSet<>();
        for (int index = 0; index < sourceRows.size(); index++) {
            result.add(validateRow(sourceRows.get(index), index + 1, tenantId, duplicateKeys));
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> confirm(List<Map<String, Object>> sourceRows, Long tenantId) {
        List<Map<String, Object>> rows = preview(sourceRows, tenantId);
        List<String> errors = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if ("error".equals(row.get("validationStatus"))) {
                errors.add("第 " + row.get("rowNo") + " 行：" + row.get("validationMessage"));
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("存在不能导入的数据，请修正后重试：" + String.join("；", errors));
        }

        long nextId = nextId();
        List<Map<String, Object>> saved = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long existingId = longValue(row.get("existingId"));
            if (existingId == null) {
                jdbcTemplate.update(
                        "insert into jsh_trade_shipment_item (id,shipment_id,material_id,depot_head_id,depot_item_id,quantity,purchase_amount,sales_amount,in_transit_quantity,cleared_quantity,stocked_quantity,sold_quantity,tenant_id,delete_flag) values (?,?,?,?,?,?,?,?,?,?,?,?,?,'0')",
                        nextId++, row.get("shipmentId"), row.get("materialId"), row.get("depotHeadId"), row.get("depotItemId"),
                        row.get("quantity"), row.get("purchaseAmount"), row.get("salesAmount"), row.get("inTransitQuantity"),
                        row.get("clearedQuantity"), row.get("stockedQuantity"), row.get("lockedQuantity"), tenantId);
            } else {
                jdbcTemplate.update(
                        "update jsh_trade_shipment_item set quantity=?,purchase_amount=?,sales_amount=?,in_transit_quantity=?,cleared_quantity=?,stocked_quantity=?,sold_quantity=? where id=? and tenant_id=?",
                        row.get("quantity"), row.get("purchaseAmount"), row.get("salesAmount"), row.get("inTransitQuantity"),
                        row.get("clearedQuantity"), row.get("stockedQuantity"), row.get("lockedQuantity"), existingId, tenantId);
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rowNo", row.get("rowNo"));
            item.put("shipmentNo", row.get("shipmentNo"));
            item.put("barCode", row.get("barCode"));
            item.put("action", existingId == null ? "新增" : "更新");
            saved.add(item);
        }
        return saved;
    }

    private Map<String, Object> validateRow(Map<String, Object> source, int rowNo, Long tenantId, Set<String> duplicateKeys) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("rowNo", rowNo);
        String shipmentNo = text(source, "shipmentNo");
        String purchaseNumber = text(source, "purchaseNumber");
        String barCode = text(source, "barCode");
        row.put("shipmentNo", shipmentNo);
        row.put("purchaseNumber", purchaseNumber);
        row.put("barCode", barCode);
        row.put("quantity", source.get("quantity"));
        row.put("inTransitQuantity", source.get("inTransitQuantity"));
        row.put("clearedQuantity", source.get("clearedQuantity"));
        row.put("stockedQuantity", source.get("stockedQuantity"));
        row.put("lockedQuantity", source.get("lockedQuantity") == null ? source.get("soldQuantity") : source.get("lockedQuantity"));
        row.put("salesAmount", source.get("salesAmount"));
        row.put("confidence", source.get("confidence"));
        row.put("validationStatus", "valid");
        row.put("validationMessage", "校验通过");

        try {
            BigDecimal quantity = decimal(source.get("quantity"));
            BigDecimal confidence = decimal(source.get("confidence"));
            row.put("quantity", quantity);
            row.put("inTransitQuantity", decimal(source.get("inTransitQuantity")));
            row.put("clearedQuantity", decimal(source.get("clearedQuantity")));
            row.put("stockedQuantity", decimal(source.get("stockedQuantity")));
            row.put("lockedQuantity", firstDecimal(source.get("lockedQuantity"), source.get("soldQuantity")));
            row.put("salesAmount", decimal(source.get("salesAmount")));
            row.put("confidence", confidence);
            if (isBlank(shipmentNo) || isBlank(purchaseNumber) || isBlank(barCode)) {
                throw new IllegalArgumentException("发运批次号、采购单号和商品条码不能为空");
            }
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("发运数量必须大于 0");
            }
            List<Map<String, Object>> shipments = jdbcTemplate.queryForList(
                    "select id as shipment_id,status from jsh_trade_shipment where shipment_no=? and tenant_id=? and coalesce(delete_flag,'0')<>'1'",
                    shipmentNo, tenantId);
            if (shipments.isEmpty()) throw new IllegalArgumentException("发运批次不存在：" + shipmentNo);
            Map<String, Object> shipment = shipments.get(0);
            Long shipmentId = longValue(shipment.get("shipment_id"));

            List<Map<String, Object>> purchases = jdbcTemplate.queryForList(
                    "select i.id as depot_item_id,i.header_id as depot_head_id,i.material_id,m.name as material_name," +
                            "coalesce(i.oper_number,0) as purchase_quantity,coalesce(i.purchase_unit_price,i.unit_price,0) as unit_price " +
                            "from jsh_depot_item i join jsh_depot_head h on h.id=i.header_id " +
                            "join jsh_material m on m.id=i.material_id " +
                            "join jsh_material_extend me on me.material_id=i.material_id and me.bar_code=? and me.default_flag='1' and coalesce(me.delete_flag,'0')<>'1' " +
                            "where h.number=? and i.tenant_id=? and h.tenant_id=? and h.type='入库' and h.sub_type='采购' " +
                            "and coalesce(i.delete_flag,'0')<>'1' and coalesce(h.delete_flag,'0')<>'1' order by i.id",
                    barCode, purchaseNumber, tenantId, tenantId);
            if (purchases.isEmpty()) {
                throw new IllegalArgumentException("找不到采购单 " + purchaseNumber + " 对应的商品条码 " + barCode);
            }
            Map<String, Object> purchase = purchases.get(0);
            Long depotItemId = longValue(purchase.get("depot_item_id"));
            if (!duplicateKeys.add(shipmentId + "-" + depotItemId)) {
                throw new IllegalArgumentException("同一发运批次和采购明细重复");
            }
            BigDecimal purchaseQuantity = decimal(purchase.get("purchase_quantity"));
            if (purchaseQuantity != null && quantity.compareTo(purchaseQuantity) > 0) {
                throw new IllegalArgumentException("发运数量不能超过采购数量 " + purchaseQuantity.stripTrailingZeros().toPlainString());
            }

            List<Map<String, Object>> existingRows = jdbcTemplate.queryForList(
                    "select id,in_transit_quantity,cleared_quantity,stocked_quantity,sold_quantity,sales_amount " +
                            "from jsh_trade_shipment_item where shipment_id=? and depot_item_id=? and tenant_id=? and coalesce(delete_flag,'0')<>'1' order by id",
                    shipmentId, depotItemId, tenantId);
            Map<String, Object> existing = existingRows.isEmpty() ? null : existingRows.get(0);
            String status = String.valueOf(shipment.get("status"));
            BigDecimal inTransit = valueOrDefault(row.get("inTransitQuantity"), existing, "in_transit_quantity", itemInTransit(status, quantity));
            BigDecimal cleared = valueOrDefault(row.get("clearedQuantity"), existing, "cleared_quantity", itemCleared(status, quantity));
            BigDecimal stocked = valueOrDefault(row.get("stockedQuantity"), existing, "stocked_quantity", itemStocked(status, quantity));
            BigDecimal locked = valueOrDefault(row.get("lockedQuantity"), existing, "sold_quantity", BigDecimal.ZERO);
            BigDecimal salesAmount = valueOrDefault(row.get("salesAmount"), existing, "sales_amount", BigDecimal.ZERO);
            validateNumbers(quantity, inTransit, cleared, stocked, locked, salesAmount);

            row.put("shipmentId", shipmentId);
            row.put("depotItemId", depotItemId);
            row.put("depotHeadId", longValue(purchase.get("depot_head_id")));
            row.put("materialId", longValue(purchase.get("material_id")));
            row.put("materialName", purchase.get("material_name"));
            row.put("purchaseAmount", decimal(purchase.get("unit_price")).multiply(quantity));
            row.put("inTransitQuantity", inTransit);
            row.put("clearedQuantity", cleared);
            row.put("stockedQuantity", stocked);
            row.put("lockedQuantity", locked);
            row.put("salesAmount", salesAmount);
            row.put("existingId", existing == null ? null : longValue(existing.get("id")));
            row.put("action", existing == null ? "新增" : "更新");
            if (confidence != null && confidence.compareTo(new BigDecimal("0.75")) < 0) {
                row.put("validationStatus", "warning");
                row.put("validationMessage", "AI 识别置信度较低，请人工核对");
            }
        } catch (Exception e) {
            row.put("validationStatus", "error");
            row.put("validationMessage", e.getMessage() == null ? "数据校验失败" : e.getMessage());
        }
        return row;
    }

    private void validateNumbers(BigDecimal quantity, BigDecimal inTransit, BigDecimal cleared,
                                 BigDecimal stocked, BigDecimal locked, BigDecimal salesAmount) {
        if (negative(inTransit) || negative(cleared) || negative(stocked) || negative(locked) || negative(salesAmount)) {
            throw new IllegalArgumentException("库存数量和销售金额不能为负数");
        }
        if (inTransit.compareTo(quantity) > 0 || cleared.compareTo(quantity) > 0 || stocked.compareTo(quantity) > 0) {
            throw new IllegalArgumentException("在途、清关或已入库数量不能超过发运数量");
        }
        if (locked.compareTo(stocked) > 0) {
            throw new IllegalArgumentException("客户锁定数量不能超过已入库数量");
        }
    }

    private BigDecimal valueOrDefault(Object value, Map<String, Object> existing, String key, BigDecimal defaultValue) {
        BigDecimal result = decimal(value);
        if (result != null) return result;
        if (existing != null && existing.get(key) != null) return decimal(existing.get(key));
        return defaultValue;
    }

    private BigDecimal firstDecimal(Object first, Object second) {
        BigDecimal value = decimal(first);
        return value == null ? decimal(second) : value;
    }

    private BigDecimal itemInTransit(String status, BigDecimal quantity) {
        return "在途".equals(status) || "已到港".equals(status) ? quantity : BigDecimal.ZERO;
    }

    private BigDecimal itemCleared(String status, BigDecimal quantity) {
        return "清关中".equals(status) || "已入库".equals(status) || "已完成".equals(status) ? quantity : BigDecimal.ZERO;
    }

    private BigDecimal itemStocked(String status, BigDecimal quantity) {
        return "已入库".equals(status) || "已完成".equals(status) ? quantity : BigDecimal.ZERO;
    }

    private long nextId() {
        Long id = jdbcTemplate.queryForObject("select coalesce(max(id),0)+1 from jsh_trade_shipment_item", Long.class);
        return id == null ? 1L : id;
    }

    private String text(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private BigDecimal decimal(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty() || "null".equalsIgnoreCase(String.valueOf(value))) return null;
        try {
            return new BigDecimal(String.valueOf(value).replace(",", "").trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("数量或金额不是有效数字：" + value);
        }
    }

    private Long longValue(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private boolean negative(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) < 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
