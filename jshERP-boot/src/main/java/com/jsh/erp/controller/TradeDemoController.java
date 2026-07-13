package com.jsh.erp.controller;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.base.BaseController;
import com.jsh.erp.utils.ErpInfo;
import com.jsh.erp.utils.Tools;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.jsh.erp.utils.ResponseJsonUtil.returnJson;

/**
 * 外贸手表 Demo 查询接口。
 *
 * 第一阶段只提供可演示的只读视图；所有数据按租户隔离，超级管理员查看已有的演示租户数据。
 */
@RestController
@RequestMapping("/trade")
public class TradeDemoController extends BaseController {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/dashboard/overview")
    public String overview(HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        Map<String, Object> data = new HashMap<>();
        data.put("purchaseAmount", amount("select coalesce(sum(purchase_amount),0) from jsh_trade_shipment_item where tenant_id=? and coalesce(delete_flag,'0')<>'1'", tenantId));
        data.put("salesAmount", amount("select coalesce(sum(total_price),0) from jsh_depot_head where tenant_id=? and type='出库' and sub_type in ('销售','零售') and coalesce(delete_flag,'0')<>'1'", tenantId));
        data.put("inTransitAmount", amount("select coalesce(sum(i.purchase_amount),0) from jsh_trade_shipment s join jsh_trade_shipment_item i on i.shipment_id=s.id and coalesce(i.delete_flag,'0')<>'1' where s.tenant_id=? and s.status='在途' and coalesce(s.delete_flag,'0')<>'1'", tenantId));
        data.put("customsAmount", amount("select coalesce(sum(i.purchase_amount),0) from jsh_trade_shipment s join jsh_trade_shipment_item i on i.shipment_id=s.id and coalesce(i.delete_flag,'0')<>'1' where s.tenant_id=? and s.status='清关中' and coalesce(s.delete_flag,'0')<>'1'", tenantId));
        data.put("availableStockAmount", amount("select coalesce(sum(i.purchase_amount * case when i.quantity=0 then 0 else greatest(i.stocked_quantity-i.sold_quantity,0)/i.quantity end),0) from jsh_trade_shipment_item i where i.tenant_id=? and coalesce(i.delete_flag,'0')<>'1'", tenantId));
        data.put("lockedStockAmount", amount("select coalesce(sum(i.purchase_amount * case when i.quantity=0 then 0 else i.sold_quantity/i.quantity end),0) from jsh_trade_shipment_item i where i.tenant_id=? and coalesce(i.delete_flag,'0')<>'1'", tenantId));
        data.put("lockedSalesAmount", amount("select coalesce(sum(sales_amount),0) from jsh_trade_shipment_item where tenant_id=? and coalesce(delete_flag,'0')<>'1'", tenantId));
        data.put("availableQuantity", amount("select coalesce(sum(greatest(stocked_quantity-sold_quantity,0)),0) from jsh_trade_shipment_item where tenant_id=? and coalesce(delete_flag,'0')<>'1'", tenantId));
        data.put("lockedQuantity", amount("select coalesce(sum(sold_quantity),0) from jsh_trade_shipment_item where tenant_id=? and coalesce(delete_flag,'0')<>'1'", tenantId));
        data.put("receivableAmount", amount("select coalesce(sum(all_need_get),0) from jsh_supplier where tenant_id=? and type='客户' and coalesce(delete_flag,'0')<>'1'", tenantId));
        data.put("landedCost", amount("select coalesce(sum(landed_cost),0) from jsh_trade_material_cost where tenant_id=? and coalesce(delete_flag,'0')<>'1'", tenantId));
        BigDecimal salesAmount = (BigDecimal) data.get("salesAmount");
        BigDecimal costOfSales = amount("select coalesce(sum(c.landed_cost * case when i.quantity=0 then 0 else i.sold_quantity/i.quantity end),0) from jsh_trade_material_cost c join jsh_trade_shipment_item i on i.shipment_id=c.shipment_id and i.material_id=c.material_id and coalesce(i.delete_flag,'0')<>'1' where c.tenant_id=? and coalesce(c.delete_flag,'0')<>'1'", tenantId);
        data.put("costOfSales", costOfSales);
        BigDecimal grossProfit = salesAmount.subtract(costOfSales);
        data.put("grossProfit", grossProfit);
        data.put("grossMargin", salesAmount.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : grossProfit.multiply(BigDecimal.valueOf(100)).divide(salesAmount, 2, BigDecimal.ROUND_HALF_UP));
        data.put("shipmentCount", count("select count(*) from jsh_trade_shipment where tenant_id=? and coalesce(delete_flag,'0')<>'1'", tenantId));
        data.put("exceptionCount", count("select count(*) from jsh_trade_document where tenant_id=? and status in ('缺失','异常') and coalesce(delete_flag,'0')<>'1'", tenantId));
        data.put("transitRiskDocumentCount", count("select count(*) from jsh_trade_document d join jsh_trade_shipment s on s.id=d.shipment_id where d.tenant_id=? and s.status in ('在途','已到港','清关中') and d.status in ('缺失','异常','审核中') and coalesce(d.delete_flag,'0')<>'1' and coalesce(s.delete_flag,'0')<>'1'", tenantId));
        data.put("currency", "CNY");
        data.put("demoRateNote", "汇率为演示固定汇率，不代表实时市场汇率");
        return ok(data);
    }

    @GetMapping("/shipment/list")
    public String shipmentList(@RequestParam(value = "status", required = false) String status, HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        StringBuilder sql = new StringBuilder("select s.id, s.shipment_no shipmentNo, s.container_no containerNo, s.bill_of_lading_no billOfLadingNo, s.origin_port originPort, s.destination_port destinationPort, s.carrier_name carrierName, s.incoterms, s.currency, s.exchange_rate exchangeRate, s.departure_date departureDate, s.estimated_arrival_date estimatedArrivalDate, s.actual_arrival_date actualArrivalDate, s.status, s.remark, coalesce(sum(i.quantity),0) quantity, coalesce(sum(i.purchase_amount),0) purchaseAmount from jsh_trade_shipment s left join jsh_trade_shipment_item i on i.shipment_id=s.id and coalesce(i.delete_flag,'0')<>'1' where s.tenant_id=? and coalesce(s.delete_flag,'0')<>'1'");
        List<Object> params = new ArrayList<>();
        params.add(tenantId);
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" and s.status=?");
            params.add(status.trim());
        }
        sql.append(" group by s.id order by s.departure_date desc, s.id desc");
        return ok(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    @GetMapping("/shipment/detail")
    public String shipmentDetail(@RequestParam("id") Long id, HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        List<Map<String, Object>> headers = jdbcTemplate.queryForList("select id, shipment_no shipmentNo, container_no containerNo, bill_of_lading_no billOfLadingNo, origin_port originPort, destination_port destinationPort, carrier_name carrierName, incoterms, currency, exchange_rate exchangeRate, departure_date departureDate, estimated_arrival_date estimatedArrivalDate, actual_arrival_date actualArrivalDate, status, remark from jsh_trade_shipment where id=? and tenant_id=? and coalesce(delete_flag,'0')<>'1'", id, tenantId);
        Map<String, Object> data = new HashMap<>();
        data.put("info", headers.isEmpty() ? null : headers.get(0));
        data.put("items", jdbcTemplate.queryForList("select i.id, i.material_id materialId, i.depot_head_id depotHeadId, i.depot_item_id depotItemId, h.number purchaseNumber, i.sales_depot_head_id salesDepotHeadId, i.sales_depot_item_id salesDepotItemId, sh.number salesNumber, sp.supplier salesCustomerName, m.name materialName, m.model, m.brand, i.quantity, i.purchase_amount purchaseAmount, i.sales_amount salesAmount, i.in_transit_quantity inTransitQuantity, i.cleared_quantity clearedQuantity, i.stocked_quantity stockedQuantity, i.sold_quantity soldQuantity from jsh_trade_shipment_item i left join jsh_depot_head h on h.id=i.depot_head_id left join jsh_depot_head sh on sh.id=i.sales_depot_head_id left join jsh_supplier sp on sp.id=sh.organ_id left join jsh_material m on m.id=i.material_id where i.shipment_id=? and i.tenant_id=? and coalesce(i.delete_flag,'0')<>'1' order by i.id", id, tenantId));
        data.put("documents", jdbcTemplate.queryForList("select id, document_type documentType, document_no documentNo, status, owner_name ownerName, due_date dueDate, attachment_name attachmentName, exception_note exceptionNote from jsh_trade_document where shipment_id=? and tenant_id=? and coalesce(delete_flag,'0')<>'1' order by id", id, tenantId));
        data.put("costs", jdbcTemplate.queryForList("select id, cost_type costType, original_amount originalAmount, currency, exchange_rate exchangeRate, cny_amount cnyAmount, allocated_flag allocatedFlag, cost_date costDate, remark from jsh_trade_cost where shipment_id=? and tenant_id=? and coalesce(delete_flag,'0')<>'1' order by id", id, tenantId));
        return ok(data);
    }

    @GetMapping("/shipment/purchase-options")
    public String purchaseOptions(HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        String sql = "select h.id depotHeadId,h.number purchaseNumber,i.id depotItemId,i.material_id materialId,m.name materialName,m.model,i.oper_number purchaseQuantity,coalesce(i.purchase_unit_price,i.unit_price,0) purchaseUnitPrice,i.all_price purchaseAmount from jsh_depot_head h join jsh_depot_item i on i.header_id=h.id and coalesce(i.delete_flag,'0')<>'1' join jsh_material m on m.id=i.material_id where h.tenant_id=? and h.type='入库' and h.sub_type='采购' and coalesce(h.delete_flag,'0')<>'1' order by h.oper_time desc,h.id desc,i.id";
        return ok(jdbcTemplate.queryForList(sql, tenantId));
    }

    @GetMapping("/shipment/sales-options")
    public String salesOptions(@RequestParam(value = "materialId", required = false) Long materialId, HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        List<Object> params = new ArrayList<>();
        params.add(tenantId);
        StringBuilder sql = new StringBuilder("select h.id salesDepotHeadId,h.number salesNumber,h.sub_type salesType,h.oper_time salesTime,s.supplier salesCustomerName,i.id salesDepotItemId,i.material_id materialId,m.name materialName,m.model,coalesce(i.oper_number,0) salesQuantity,coalesce(i.unit_price,0) salesUnitPrice,coalesce(i.all_price,0) salesAmount from jsh_depot_head h join jsh_depot_item i on i.header_id=h.id and coalesce(i.delete_flag,'0')<>'1' join jsh_material m on m.id=i.material_id left join jsh_supplier s on s.id=h.organ_id where h.tenant_id=? and h.type='出库' and h.sub_type in ('销售订单','销售','零售') and coalesce(h.delete_flag,'0')<>'1'");
        if (materialId != null) {
            sql.append(" and i.material_id=?");
            params.add(materialId);
        }
        sql.append(" order by h.oper_time desc,h.id desc,i.id");
        return ok(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    @PostMapping("/shipment/item/add")
    @Transactional(rollbackFor = Exception.class)
    public String addShipmentItem(@RequestBody JSONObject obj, HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        Long shipmentId = obj.getLong("shipmentId");
        Long depotItemId = obj.getLong("depotItemId");
        BigDecimal quantity = decimal(obj, "quantity", BigDecimal.ZERO);
        if (shipmentId == null || depotItemId == null || quantity.compareTo(BigDecimal.ZERO) <= 0 || !exists("jsh_trade_shipment", shipmentId, tenantId)) return tradeError("请选择发运批次、采购明细并填写数量");
        List<Map<String, Object>> purchaseRows = jdbcTemplate.queryForList("select i.id depot_item_id,i.header_id depot_head_id,i.material_id,coalesce(i.oper_number,0) oper_number,coalesce(i.purchase_unit_price,i.unit_price,0) unit_price from jsh_depot_item i join jsh_depot_head h on h.id=i.header_id where i.id=? and i.tenant_id=? and h.tenant_id=? and h.type='入库' and h.sub_type='采购' and coalesce(i.delete_flag,'0')<>'1' and coalesce(h.delete_flag,'0')<>'1'", depotItemId, tenantId, tenantId);
        if (purchaseRows.isEmpty()) return tradeError("关联的采购明细不存在");
        Map<String, Object> purchase = purchaseRows.get(0);
        BigDecimal allocated = amount("select coalesce(sum(quantity),0) from jsh_trade_shipment_item where depot_item_id=? and tenant_id=? and coalesce(delete_flag,'0')<>'1'", depotItemId, tenantId);
        BigDecimal available = ((BigDecimal) purchase.get("oper_number")).subtract(allocated);
        if (quantity.compareTo(available) > 0) return tradeError("可发运数量不足，当前最多可关联 " + available.stripTrailingZeros().toPlainString());
        String status = jdbcTemplate.queryForObject("select status from jsh_trade_shipment where id=? and tenant_id=?", String.class, shipmentId, tenantId);
        long id = nextId("jsh_trade_shipment_item");
        BigDecimal unitPrice = (BigDecimal) purchase.get("unit_price");
        jdbcTemplate.update("insert into jsh_trade_shipment_item (id,shipment_id,material_id,depot_head_id,depot_item_id,quantity,purchase_amount,in_transit_quantity,cleared_quantity,stocked_quantity,sold_quantity,tenant_id,delete_flag) values (?,?,?,?,?,?,?,?,?,?,?,?, '0')", id, shipmentId, ((Number) purchase.get("material_id")).longValue(), ((Number) purchase.get("depot_head_id")).longValue(), depotItemId, quantity, unitPrice.multiply(quantity), itemInTransit(status, quantity), itemCleared(status, quantity), itemStocked(status, quantity), BigDecimal.ZERO, tenantId);
        return ok(singleValue("id", id));
    }

    @PutMapping("/shipment/item/update")
    public String updateShipmentItem(@RequestBody JSONObject obj, HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        Long id = obj.getLong("id");
        BigDecimal quantity = decimal(obj, "quantity", BigDecimal.ZERO);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select i.id,i.shipment_id,i.depot_item_id,coalesce(d.oper_number,0) purchase_quantity,coalesce(d.purchase_unit_price,d.unit_price,0) unit_price,s.status from jsh_trade_shipment_item i join jsh_trade_shipment s on s.id=i.shipment_id left join jsh_depot_item d on d.id=i.depot_item_id where i.id=? and i.tenant_id=? and coalesce(i.delete_flag,'0')<>'1'", id, tenantId);
        if (rows.isEmpty() || quantity.compareTo(BigDecimal.ZERO) <= 0) return tradeError("发运明细不存在或数量无效");
        Map<String, Object> row = rows.get(0);
        if (row.get("depot_item_id") == null) return tradeError("该演示明细尚未关联采购单，请新增关联 SKU 后再修改");
        Long depotItemId = ((Number) row.get("depot_item_id")).longValue();
        BigDecimal otherAllocated = amount("select coalesce(sum(quantity),0) from jsh_trade_shipment_item where depot_item_id=? and id<>? and tenant_id=? and coalesce(delete_flag,'0')<>'1'", depotItemId, id, tenantId);
        if (quantity.add(otherAllocated).compareTo((BigDecimal) row.get("purchase_quantity")) > 0) return tradeError("数量超过该采购明细可发运数量");
        String status = String.valueOf(row.get("status"));
        BigDecimal unitPrice = (BigDecimal) row.get("unit_price");
        jdbcTemplate.update("update jsh_trade_shipment_item set quantity=?,purchase_amount=?,in_transit_quantity=?,cleared_quantity=?,stocked_quantity=? where id=? and tenant_id=?", quantity, unitPrice.multiply(quantity), itemInTransit(status, quantity), itemCleared(status, quantity), itemStocked(status, quantity), id, tenantId);
        return ok(singleValue("id", id));
    }

    @PostMapping("/shipment/item/delete")
    public String deleteShipmentItem(@RequestBody JSONObject obj, HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        Long id = obj.getLong("id");
        int updated = jdbcTemplate.update("update jsh_trade_shipment_item set delete_flag='1' where id=? and tenant_id=? and coalesce(delete_flag,'0')<>'1'", id, tenantId);
        return updated > 0 ? ok(singleValue("id", id)) : tradeError("发运明细不存在");
    }

    @PostMapping("/shipment/item/sales-link")
    public String linkShipmentItemSales(@RequestBody JSONObject obj, HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        Long id = obj.getLong("id");
        Long salesDepotItemId = obj.getLong("salesDepotItemId");
        BigDecimal soldQuantity = decimal(obj, "soldQuantity", BigDecimal.ZERO);
        if (id == null || salesDepotItemId == null || soldQuantity.compareTo(BigDecimal.ZERO) <= 0) return tradeError("请选择发运明细、销售单明细并填写锁定数量");
        List<Map<String, Object>> items = jdbcTemplate.queryForList("select id,material_id,coalesce(stocked_quantity,0) stocked_quantity from jsh_trade_shipment_item where id=? and tenant_id=? and coalesce(delete_flag,'0')<>'1'", id, tenantId);
        if (items.isEmpty()) return tradeError("发运明细不存在");
        Map<String, Object> item = items.get(0);
        if (soldQuantity.compareTo((BigDecimal) item.get("stocked_quantity")) > 0) return tradeError("锁定数量不能超过已入库数量");
        List<Map<String, Object>> salesRows = jdbcTemplate.queryForList("select i.id sales_depot_item_id,i.header_id sales_depot_head_id,i.material_id,coalesce(i.oper_number,0) oper_number,coalesce(i.unit_price,0) unit_price from jsh_depot_item i join jsh_depot_head h on h.id=i.header_id where i.id=? and i.tenant_id=? and h.tenant_id=? and h.type='出库' and h.sub_type in ('销售订单','销售','零售') and coalesce(i.delete_flag,'0')<>'1' and coalesce(h.delete_flag,'0')<>'1'", salesDepotItemId, tenantId, tenantId);
        if (salesRows.isEmpty()) return tradeError("销售明细不存在");
        Map<String, Object> sales = salesRows.get(0);
        if (!String.valueOf(item.get("material_id")).equals(String.valueOf(sales.get("material_id")))) return tradeError("销售明细 SKU 与发运 SKU 不一致");
        if (soldQuantity.compareTo((BigDecimal) sales.get("oper_number")) > 0) return tradeError("锁定数量不能超过销售明细数量");
        BigDecimal unitPrice = (BigDecimal) sales.get("unit_price");
        jdbcTemplate.update("update jsh_trade_shipment_item set sales_depot_head_id=?,sales_depot_item_id=?,sold_quantity=?,sales_amount=? where id=? and tenant_id=?",
                ((Number) sales.get("sales_depot_head_id")).longValue(), salesDepotItemId, soldQuantity, unitPrice.multiply(soldQuantity), id, tenantId);
        return ok(singleValue("id", id));
    }

    @PostMapping("/shipment/item/sales-unlink")
    public String unlinkShipmentItemSales(@RequestBody JSONObject obj, HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        Long id = obj.getLong("id");
        int updated = jdbcTemplate.update("update jsh_trade_shipment_item set sales_depot_head_id=null,sales_depot_item_id=null,sold_quantity=0,sales_amount=0 where id=? and tenant_id=? and coalesce(delete_flag,'0')<>'1'", id, tenantId);
        return updated > 0 ? ok(singleValue("id", id)) : tradeError("发运明细不存在");
    }

    @GetMapping("/document/list")
    public String documentList(@RequestParam(value = "status", required = false) String status, HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        StringBuilder sql = new StringBuilder("select d.id, s.shipment_no shipmentNo, d.document_type documentType, d.document_no documentNo, d.status, d.owner_name ownerName, d.due_date dueDate, d.attachment_name attachmentName, d.exception_note exceptionNote from jsh_trade_document d join jsh_trade_shipment s on s.id=d.shipment_id where d.tenant_id=? and coalesce(d.delete_flag,'0')<>'1'");
        List<Object> params = new ArrayList<>();
        params.add(tenantId);
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" and d.status=?");
            params.add(status.trim());
        }
        sql.append(" order by case when d.status in ('缺失','异常') then 0 else 1 end, d.due_date, d.id");
        return ok(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    @GetMapping("/inventory/status")
    public String inventoryStatus(HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        String sql = "select status_name statusName, coalesce(sum(quantity),0) quantity, coalesce(sum(amount),0) amount from (" +
                "select '海运在途' status_name, i.in_transit_quantity quantity, i.purchase_amount * case when i.quantity=0 then 0 else i.in_transit_quantity/i.quantity end amount from jsh_trade_shipment s join jsh_trade_shipment_item i on i.shipment_id=s.id where s.tenant_id=? and s.status='在途' and coalesce(s.delete_flag,'0')<>'1' and coalesce(i.delete_flag,'0')<>'1' union all " +
                "select '清关中' status_name, i.cleared_quantity quantity, i.purchase_amount * case when i.quantity=0 then 0 else i.cleared_quantity/i.quantity end amount from jsh_trade_shipment s join jsh_trade_shipment_item i on i.shipment_id=s.id where s.tenant_id=? and s.status='清关中' and coalesce(s.delete_flag,'0')<>'1' and coalesce(i.delete_flag,'0')<>'1' union all " +
                "select '墨西哥可销售库存' status_name, i.stocked_quantity-i.sold_quantity quantity, i.purchase_amount * case when i.quantity=0 then 0 else greatest(i.stocked_quantity-i.sold_quantity,0)/i.quantity end amount from jsh_trade_shipment_item i where i.tenant_id=? and coalesce(i.delete_flag,'0')<>'1' union all " +
                "select '客户锁定库存' status_name, i.sold_quantity quantity, i.purchase_amount * case when i.quantity=0 then 0 else i.sold_quantity/i.quantity end amount from jsh_trade_shipment_item i where i.tenant_id=? and coalesce(i.delete_flag,'0')<>'1'" +
                ") x group by status_name order by case status_name when '海运在途' then 1 when '清关中' then 2 when '墨西哥可销售库存' then 3 else 4 end";
        return ok(jdbcTemplate.queryForList(sql, tenantId, tenantId, tenantId, tenantId));
    }

    @GetMapping("/inventory/detail")
    public String inventoryDetail(@RequestParam(value = "status", required = false) String status, HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        List<Object> params = new ArrayList<>();
        params.add(tenantId);
        StringBuilder sql = new StringBuilder("select s.shipment_no shipmentNo,s.status shipmentStatus,i.material_id materialId,m.name materialName,m.model,m.brand,coalesce(i.quantity,0) shipmentQuantity,coalesce(i.in_transit_quantity,0) inTransitQuantity,coalesce(i.cleared_quantity,0) clearedQuantity,coalesce(i.stocked_quantity,0) stockedQuantity,coalesce(i.sold_quantity,0) lockedQuantity,greatest(coalesce(i.stocked_quantity,0)-coalesce(i.sold_quantity,0),0) availableQuantity,coalesce(i.purchase_amount,0) purchaseAmount,coalesce(i.sales_amount,0) salesAmount,ph.number purchaseNumber,sh.number salesNumber,sp.supplier salesCustomerName,case when coalesce(i.in_transit_quantity,0)>0 then '海运在途' when coalesce(i.cleared_quantity,0)>0 and coalesce(i.stocked_quantity,0)=0 then '清关中' when greatest(coalesce(i.stocked_quantity,0)-coalesce(i.sold_quantity,0),0)>0 then '墨西哥可销售库存' when coalesce(i.sold_quantity,0)>0 then '客户锁定库存' else '已消耗' end statusName from jsh_trade_shipment_item i join jsh_trade_shipment s on s.id=i.shipment_id left join jsh_material m on m.id=i.material_id left join jsh_depot_head ph on ph.id=i.depot_head_id left join jsh_depot_head sh on sh.id=i.sales_depot_head_id left join jsh_supplier sp on sp.id=sh.organ_id where i.tenant_id=? and coalesce(i.delete_flag,'0')<>'1' and coalesce(s.delete_flag,'0')<>'1'");
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" and (case when coalesce(i.in_transit_quantity,0)>0 then '海运在途' when coalesce(i.cleared_quantity,0)>0 and coalesce(i.stocked_quantity,0)=0 then '清关中' when greatest(coalesce(i.stocked_quantity,0)-coalesce(i.sold_quantity,0),0)>0 then '墨西哥可销售库存' when coalesce(i.sold_quantity,0)>0 then '客户锁定库存' else '已消耗' end)=?");
            params.add(status.trim());
        }
        sql.append(" order by s.departure_date desc,s.id desc,m.name,i.id");
        return ok(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    @GetMapping("/cost/profit")
    public String costProfit(HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        String sql = "select s.shipment_no shipmentNo, m.name materialName, m.model, c.purchase_cost purchaseCost, c.logistics_cost logisticsCost, c.duty_cost dutyCost, c.local_cost localCost, c.landed_cost landedCost, coalesce(i.quantity,0) shipmentQuantity, coalesce(i.sold_quantity,0) soldQuantity, coalesce(i.sales_amount,0) salesAmount, sh.number salesNumber, sp.supplier salesCustomerName, round(c.landed_cost * case when coalesce(i.quantity,0)=0 then 0 else coalesce(i.sold_quantity,0)/i.quantity end, 2) soldLandedCost, round(coalesce(i.sales_amount,0) - c.landed_cost * case when coalesce(i.quantity,0)=0 then 0 else coalesce(i.sold_quantity,0)/i.quantity end, 2) actualGrossProfit, case when coalesce(i.sales_amount,0)=0 then 0 else round((coalesce(i.sales_amount,0) - c.landed_cost * case when coalesce(i.quantity,0)=0 then 0 else coalesce(i.sold_quantity,0)/i.quantity end) * 100 / i.sales_amount, 2) end actualGrossMargin, round(c.landed_cost * 1.35, 2) estimatedSales, round(c.landed_cost * 0.35, 2) estimatedGrossProfit, 25.93 grossMargin from jsh_trade_material_cost c join jsh_trade_shipment s on s.id=c.shipment_id join jsh_material m on m.id=c.material_id left join jsh_trade_shipment_item i on i.shipment_id=c.shipment_id and i.material_id=c.material_id and coalesce(i.delete_flag,'0')<>'1' left join jsh_depot_head sh on sh.id=i.sales_depot_head_id left join jsh_supplier sp on sp.id=sh.organ_id where c.tenant_id=? and coalesce(c.delete_flag,'0')<>'1' order by coalesce(i.sales_amount,0) desc,c.landed_cost desc";
        return ok(jdbcTemplate.queryForList(sql, tenantId));
    }

    @PostMapping("/shipment/add")
    @Transactional(rollbackFor = Exception.class)
    public String addShipment(@RequestBody JSONObject obj, HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        String shipmentNo = obj.getString("shipmentNo");
        if (isBlank(shipmentNo) || isBlank(obj.getString("originPort")) || isBlank(obj.getString("destinationPort"))) {
            return tradeError("请填写批次号、起运港和目的港");
        }
        Long duplicate = count("select count(*) from jsh_trade_shipment where tenant_id=? and shipment_no=? and coalesce(delete_flag,'0')<>'1'", tenantId, shipmentNo);
        if (duplicate > 0) {
            return tradeError("发运批次号已存在");
        }
        long id = nextId("jsh_trade_shipment");
        jdbcTemplate.update("insert into jsh_trade_shipment (id,shipment_no,container_no,bill_of_lading_no,origin_port,destination_port,carrier_name,incoterms,currency,exchange_rate,departure_date,estimated_arrival_date,actual_arrival_date,status,remark,tenant_id,delete_flag) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, '0')",
                id, shipmentNo.trim(), obj.getString("containerNo"), obj.getString("billOfLadingNo"), obj.getString("originPort"), obj.getString("destinationPort"), obj.getString("carrierName"), defaultString(obj.getString("incoterms"), "CIF"), defaultString(obj.getString("currency"), "USD"), decimal(obj, "exchangeRate", BigDecimal.valueOf(7.2)), timestamp(obj.getString("departureDate")), timestamp(obj.getString("estimatedArrivalDate")), null, "待订舱", obj.getString("remark"), tenantId);
        return ok(singleValue("id", id));
    }

    @PutMapping("/shipment/update")
    public String updateShipment(@RequestBody JSONObject obj, HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        Long id = obj.getLong("id");
        if (id == null || !exists("jsh_trade_shipment", id, tenantId)) return tradeError("发运批次不存在");
        jdbcTemplate.update("update jsh_trade_shipment set container_no=?,bill_of_lading_no=?,origin_port=?,destination_port=?,carrier_name=?,incoterms=?,currency=?,exchange_rate=?,departure_date=?,estimated_arrival_date=?,remark=? where id=? and tenant_id=?",
                obj.getString("containerNo"), obj.getString("billOfLadingNo"), obj.getString("originPort"), obj.getString("destinationPort"), obj.getString("carrierName"), defaultString(obj.getString("incoterms"), "CIF"), defaultString(obj.getString("currency"), "USD"), decimal(obj, "exchangeRate", BigDecimal.valueOf(7.2)), timestamp(obj.getString("departureDate")), timestamp(obj.getString("estimatedArrivalDate")), obj.getString("remark"), id, tenantId);
        return ok(singleValue("id", id));
    }

    @PostMapping("/shipment/status")
    @Transactional(rollbackFor = Exception.class)
    public String advanceShipmentStatus(@RequestBody JSONObject obj, HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        Long id = obj.getLong("id");
        String target = obj.getString("status");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select status from jsh_trade_shipment where id=? and tenant_id=? and coalesce(delete_flag,'0')<>'1'", id, tenantId);
        if (rows.isEmpty()) return tradeError("发运批次不存在");
        String current = String.valueOf(rows.get(0).get("status"));
        List<String> flow = Arrays.asList("待订舱", "已订舱", "已装柜", "已开船", "在途", "已到港", "清关中", "已入库", "已完成");
        if (!flow.contains(target)) return tradeError("不支持的发运状态");
        if (flow.indexOf(target) != flow.indexOf(current) + 1) return tradeError("状态只能按业务顺序推进：当前为" + current);
        jdbcTemplate.update("update jsh_trade_shipment set status=?,actual_arrival_date=case when ?='已到港' then current_timestamp else actual_arrival_date end where id=? and tenant_id=?", target, target, id, tenantId);
        if ("在途".equals(target)) jdbcTemplate.update("update jsh_trade_shipment_item set in_transit_quantity=quantity where shipment_id=? and tenant_id=?", id, tenantId);
        if ("清关中".equals(target)) jdbcTemplate.update("update jsh_trade_shipment_item set in_transit_quantity=0,cleared_quantity=quantity where shipment_id=? and tenant_id=?", id, tenantId);
        if ("已入库".equals(target) || "已完成".equals(target)) jdbcTemplate.update("update jsh_trade_shipment_item set in_transit_quantity=0,cleared_quantity=quantity,stocked_quantity=quantity where shipment_id=? and tenant_id=?", id, tenantId);
        return ok(singleValue("status", target));
    }

    @PostMapping("/document/add")
    public String addDocument(@RequestBody JSONObject obj, HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        Long shipmentId = obj.getLong("shipmentId");
        if (shipmentId == null || !exists("jsh_trade_shipment", shipmentId, tenantId) || isBlank(obj.getString("documentType"))) return tradeError("请选择发运批次并填写单证类型");
        long id = nextId("jsh_trade_document");
        jdbcTemplate.update("insert into jsh_trade_document (id,shipment_id,document_type,document_no,status,owner_name,due_date,attachment_name,exception_note,tenant_id,delete_flag) values (?,?,?,?,?,?,?,?,?,?, '0')", id, shipmentId, obj.getString("documentType"), obj.getString("documentNo"), defaultString(obj.getString("status"), "待准备"), obj.getString("ownerName"), timestamp(obj.getString("dueDate")), obj.getString("attachmentName"), obj.getString("exceptionNote"), tenantId);
        return ok(singleValue("id", id));
    }

    @PutMapping("/document/update")
    public String updateDocument(@RequestBody JSONObject obj, HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        Long id = obj.getLong("id");
        if (id == null || !exists("jsh_trade_document", id, tenantId)) return tradeError("清关单证不存在");
        jdbcTemplate.update("update jsh_trade_document set document_type=?,document_no=?,status=?,owner_name=?,due_date=?,attachment_name=?,exception_note=? where id=? and tenant_id=?", obj.getString("documentType"), obj.getString("documentNo"), defaultString(obj.getString("status"), "待准备"), obj.getString("ownerName"), timestamp(obj.getString("dueDate")), obj.getString("attachmentName"), obj.getString("exceptionNote"), id, tenantId);
        return ok(singleValue("id", id));
    }

    @GetMapping("/cost/list")
    public String costList(@RequestParam(value = "shipmentId", required = false) Long shipmentId, HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        String sql = "select c.id,c.shipment_id shipmentId,s.shipment_no shipmentNo,c.cost_type costType,c.original_amount originalAmount,c.currency,c.exchange_rate exchangeRate,c.cny_amount cnyAmount,c.allocated_flag allocatedFlag,c.cost_date costDate,c.remark from jsh_trade_cost c join jsh_trade_shipment s on s.id=c.shipment_id where c.tenant_id=? and coalesce(c.delete_flag,'0')<>'1'";
        if (shipmentId != null) sql += " and c.shipment_id=" + shipmentId;
        return ok(jdbcTemplate.queryForList(sql + " order by c.cost_date desc,c.id desc", tenantId));
    }

    @PostMapping("/cost/add")
    public String addCost(@RequestBody JSONObject obj, HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        Long shipmentId = obj.getLong("shipmentId");
        if (shipmentId == null || !exists("jsh_trade_shipment", shipmentId, tenantId) || isBlank(obj.getString("costType"))) return tradeError("请选择发运批次并填写费用类型");
        BigDecimal originalAmount = decimal(obj, "originalAmount", BigDecimal.ZERO);
        BigDecimal exchangeRate = decimal(obj, "exchangeRate", BigDecimal.ONE);
        long id = nextId("jsh_trade_cost");
        jdbcTemplate.update("insert into jsh_trade_cost (id,shipment_id,cost_type,original_amount,currency,exchange_rate,cny_amount,allocation_method,allocated_flag,cost_date,remark,tenant_id,delete_flag) values (?,?,?,?,?,?,?,?,?,?,?,?, '0')", id, shipmentId, obj.getString("costType"), originalAmount, defaultString(obj.getString("currency"), "CNY"), exchangeRate, originalAmount.multiply(exchangeRate), "PURCHASE_AMOUNT", "0", timestamp(obj.getString("costDate")), obj.getString("remark"), tenantId);
        return ok(singleValue("id", id));
    }

    @PostMapping("/cost/allocate")
    @Transactional(rollbackFor = Exception.class)
    public String allocateCost(@RequestBody JSONObject obj, HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        Long shipmentId = obj.getLong("shipmentId");
        if (shipmentId == null || !exists("jsh_trade_shipment", shipmentId, tenantId)) return tradeError("请选择有效的发运批次");
        List<Map<String, Object>> items = jdbcTemplate.queryForList("select material_id, purchase_amount from jsh_trade_shipment_item where shipment_id=? and tenant_id=? and coalesce(delete_flag,'0')<>'1' order by id", shipmentId, tenantId);
        BigDecimal totalPurchase = BigDecimal.ZERO;
        for (Map<String, Object> item : items) totalPurchase = totalPurchase.add((BigDecimal) item.get("purchase_amount"));
        if (items.isEmpty() || totalPurchase.compareTo(BigDecimal.ZERO) <= 0) return tradeError("该批次没有可分摊的 SKU 采购金额");
        BigDecimal logistics = amount("select coalesce(sum(cny_amount),0) from jsh_trade_cost where shipment_id=? and tenant_id=? and cost_type in ('国内运输','装柜费','海运费','保险费','报关费','港口费','仓储费') and coalesce(delete_flag,'0')<>'1'", shipmentId, tenantId);
        BigDecimal duty = amount("select coalesce(sum(cny_amount),0) from jsh_trade_cost where shipment_id=? and tenant_id=? and cost_type in ('关税','IVA') and coalesce(delete_flag,'0')<>'1'", shipmentId, tenantId);
        BigDecimal local = amount("select coalesce(sum(cny_amount),0) from jsh_trade_cost where shipment_id=? and tenant_id=? and cost_type in ('本地配送费','其他费用') and coalesce(delete_flag,'0')<>'1'", shipmentId, tenantId);
        long costId = nextId("jsh_trade_material_cost");
        for (Map<String, Object> item : items) {
            Long materialId = ((Number) item.get("material_id")).longValue();
            BigDecimal purchase = (BigDecimal) item.get("purchase_amount");
            BigDecimal ratio = purchase.divide(totalPurchase, 12, BigDecimal.ROUND_HALF_UP);
            BigDecimal logisticsPart = logistics.multiply(ratio).setScale(2, BigDecimal.ROUND_HALF_UP);
            BigDecimal dutyPart = duty.multiply(ratio).setScale(2, BigDecimal.ROUND_HALF_UP);
            BigDecimal localPart = local.multiply(ratio).setScale(2, BigDecimal.ROUND_HALF_UP);
            jdbcTemplate.update("insert into jsh_trade_material_cost (id,shipment_id,material_id,purchase_cost,logistics_cost,duty_cost,local_cost,landed_cost,calculated_time,tenant_id,delete_flag) values (?,?,?,?,?,?,?,?,current_timestamp,?,'0') on conflict (shipment_id,material_id) do update set purchase_cost=excluded.purchase_cost,logistics_cost=excluded.logistics_cost,duty_cost=excluded.duty_cost,local_cost=excluded.local_cost,landed_cost=excluded.landed_cost,calculated_time=current_timestamp", costId++, shipmentId, materialId, purchase, logisticsPart, dutyPart, localPart, purchase.add(logisticsPart).add(dutyPart).add(localPart), tenantId);
        }
        jdbcTemplate.update("update jsh_trade_cost set allocated_flag='1' where shipment_id=? and tenant_id=? and coalesce(delete_flag,'0')<>'1'", shipmentId, tenantId);
        return ok(singleValue("shipmentId", shipmentId));
    }

    private boolean exists(String table, Long id, Long tenantId) {
        return count("select count(*) from " + table + " where id=? and tenant_id=? and coalesce(delete_flag,'0')<>'1'", id, tenantId) > 0;
    }

    private long nextId(String table) {
        Long id = jdbcTemplate.queryForObject("select coalesce(max(id),0)+1 from " + table, Long.class);
        return id == null ? 1L : id;
    }

    private Timestamp timestamp(String value) {
        if (isBlank(value)) return null;
        String normalized = value.trim().replace('T', ' ');
        return Timestamp.valueOf(normalized.length() == 10 ? normalized + " 00:00:00" : normalized);
    }

    private BigDecimal decimal(JSONObject obj, String key, BigDecimal defaultValue) {
        BigDecimal value = obj.getBigDecimal(key);
        return value == null ? defaultValue : value;
    }

    private String defaultString(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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

    private Map<String, Object> singleValue(String key, Object value) {
        Map<String, Object> result = new HashMap<>();
        result.put(key, value);
        return result;
    }

    private String tradeError(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("data", message);
        return returnJson(result, ErpInfo.ERROR.name, ErpInfo.ERROR.code);
    }

    private Long resolveTenantId(HttpServletRequest request) {
        Long tenantId = Tools.getTenantIdByToken(request.getHeader("X-Access-Token"));
        if (tenantId != null && tenantId > 0) {
            return tenantId;
        }
        Long demoTenant = jdbcTemplate.queryForObject("select coalesce(min(tenant_id),0) from jsh_trade_shipment where coalesce(delete_flag,'0')<>'1'", Long.class);
        return demoTenant == null ? 0L : demoTenant;
    }

    private BigDecimal amount(String sql, Long tenantId) {
        BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, tenantId);
        return result == null ? BigDecimal.ZERO : result;
    }

    private BigDecimal amount(String sql, Object... params) {
        BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, params);
        return result == null ? BigDecimal.ZERO : result;
    }

    private Long count(String sql, Long tenantId) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class, tenantId);
        return result == null ? 0L : result;
    }

    private Long count(String sql, Object... params) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class, params);
        return result == null ? 0L : result;
    }

    private String ok(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("data", normalizeResponseKeys(data));
        return returnJson(result, ErpInfo.OK.name, ErpInfo.OK.code);
    }

    /**
     * PostgreSQL 会将未加双引号的别名统一折叠为小写；JdbcTemplate 返回的 Map 因此不再保留
     * SQL 中的驼峰写法。统一在接口出口恢复前端约定的字段名，避免页面出现 undefined/¥0。
     */
    @SuppressWarnings("unchecked")
    private Object normalizeResponseKeys(Object value) {
        if (value instanceof List) {
            List<Object> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(normalizeResponseKeys(item));
            }
            return result;
        }
        if (value instanceof Map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                String key = String.valueOf(entry.getKey());
                result.put(normalizeKey(key), normalizeResponseKeys(entry.getValue()));
            }
            return result;
        }
        return value;
    }

    private String normalizeKey(String key) {
        Map<String, String> keyMap = new HashMap<>();
        keyMap.put("shipmentno", "shipmentNo");
        keyMap.put("containerno", "containerNo");
        keyMap.put("billofladingno", "billOfLadingNo");
        keyMap.put("originport", "originPort");
        keyMap.put("destinationport", "destinationPort");
        keyMap.put("carriername", "carrierName");
        keyMap.put("exchangerate", "exchangeRate");
        keyMap.put("departuredate", "departureDate");
        keyMap.put("estimatedarrivaldate", "estimatedArrivalDate");
        keyMap.put("actualarrivaldate", "actualArrivalDate");
        keyMap.put("purchaseamount", "purchaseAmount");
        keyMap.put("purchasenumber", "purchaseNumber");
        keyMap.put("purchasequantity", "purchaseQuantity");
        keyMap.put("purchaseunitprice", "purchaseUnitPrice");
        keyMap.put("materialid", "materialId");
        keyMap.put("depotheadid", "depotHeadId");
        keyMap.put("depotitemid", "depotItemId");
        keyMap.put("salesdepotheadid", "salesDepotHeadId");
        keyMap.put("salesdepotitemid", "salesDepotItemId");
        keyMap.put("salesnumber", "salesNumber");
        keyMap.put("salestype", "salesType");
        keyMap.put("salestime", "salesTime");
        keyMap.put("salescustomername", "salesCustomerName");
        keyMap.put("salesquantity", "salesQuantity");
        keyMap.put("salesunitprice", "salesUnitPrice");
        keyMap.put("salesamount", "salesAmount");
        keyMap.put("materialname", "materialName");
        keyMap.put("intransitquantity", "inTransitQuantity");
        keyMap.put("clearedquantity", "clearedQuantity");
        keyMap.put("stockedquantity", "stockedQuantity");
        keyMap.put("soldquantity", "soldQuantity");
        keyMap.put("documenttype", "documentType");
        keyMap.put("documentno", "documentNo");
        keyMap.put("ownername", "ownerName");
        keyMap.put("duedate", "dueDate");
        keyMap.put("attachmentname", "attachmentName");
        keyMap.put("exceptionnote", "exceptionNote");
        keyMap.put("costtype", "costType");
        keyMap.put("originalamount", "originalAmount");
        keyMap.put("cnyamount", "cnyAmount");
        keyMap.put("allocatedflag", "allocatedFlag");
        keyMap.put("costdate", "costDate");
        keyMap.put("statusname", "statusName");
        keyMap.put("purchasecost", "purchaseCost");
        keyMap.put("logisticscost", "logisticsCost");
        keyMap.put("dutycost", "dutyCost");
        keyMap.put("localcost", "localCost");
        keyMap.put("landedcost", "landedCost");
        keyMap.put("estimatedsales", "estimatedSales");
        keyMap.put("estimatedgrossprofit", "estimatedGrossProfit");
        keyMap.put("soldlandedcost", "soldLandedCost");
        keyMap.put("actualgrossprofit", "actualGrossProfit");
        keyMap.put("actualgrossmargin", "actualGrossMargin");
        keyMap.put("lockedsalesamount", "lockedSalesAmount");
        keyMap.put("availablequantity", "availableQuantity");
        keyMap.put("transitriskdocumentcount", "transitRiskDocumentCount");
        keyMap.put("grossmargin", "grossMargin");
        keyMap.put("shipmentstatus", "shipmentStatus");
        keyMap.put("shipmentquantity", "shipmentQuantity");
        keyMap.put("lockedquantity", "lockedQuantity");
        keyMap.put("availablequantity", "availableQuantity");
        return keyMap.getOrDefault(key, key);
    }
}
