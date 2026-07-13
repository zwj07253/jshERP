package com.jsh.erp.controller;

import com.jsh.erp.base.BaseController;
import com.jsh.erp.utils.ErpInfo;
import com.jsh.erp.utils.Tools;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
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
        data.put("availableStockAmount", amount("select coalesce(sum(i.purchase_amount * case when i.quantity=0 then 0 else i.stocked_quantity/i.quantity end),0) from jsh_trade_shipment_item i where i.tenant_id=? and coalesce(i.delete_flag,'0')<>'1'", tenantId));
        data.put("lockedStockAmount", amount("select coalesce(sum(i.purchase_amount * case when i.quantity=0 then 0 else i.sold_quantity/i.quantity end),0) from jsh_trade_shipment_item i where i.tenant_id=? and coalesce(i.delete_flag,'0')<>'1'", tenantId));
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
        data.put("items", jdbcTemplate.queryForList("select i.id, i.material_id materialId, m.name materialName, m.model, m.brand, i.quantity, i.purchase_amount purchaseAmount, i.in_transit_quantity inTransitQuantity, i.cleared_quantity clearedQuantity, i.stocked_quantity stockedQuantity, i.sold_quantity soldQuantity from jsh_trade_shipment_item i left join jsh_material m on m.id=i.material_id where i.shipment_id=? and i.tenant_id=? and coalesce(i.delete_flag,'0')<>'1' order by i.id", id, tenantId));
        data.put("documents", jdbcTemplate.queryForList("select id, document_type documentType, document_no documentNo, status, owner_name ownerName, due_date dueDate, attachment_name attachmentName, exception_note exceptionNote from jsh_trade_document where shipment_id=? and tenant_id=? and coalesce(delete_flag,'0')<>'1' order by id", id, tenantId));
        data.put("costs", jdbcTemplate.queryForList("select id, cost_type costType, original_amount originalAmount, currency, exchange_rate exchangeRate, cny_amount cnyAmount, allocated_flag allocatedFlag, cost_date costDate, remark from jsh_trade_cost where shipment_id=? and tenant_id=? and coalesce(delete_flag,'0')<>'1' order by id", id, tenantId));
        return ok(data);
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

    @GetMapping("/cost/profit")
    public String costProfit(HttpServletRequest request) {
        Long tenantId = resolveTenantId(request);
        String sql = "select s.shipment_no shipmentNo, m.name materialName, m.model, c.purchase_cost purchaseCost, c.logistics_cost logisticsCost, c.duty_cost dutyCost, c.local_cost localCost, c.landed_cost landedCost, round(c.landed_cost * 1.35, 2) estimatedSales, round(c.landed_cost * 0.35, 2) estimatedGrossProfit, 25.93 grossMargin from jsh_trade_material_cost c join jsh_trade_shipment s on s.id=c.shipment_id join jsh_material m on m.id=c.material_id where c.tenant_id=? and coalesce(c.delete_flag,'0')<>'1' order by c.landed_cost desc";
        return ok(jdbcTemplate.queryForList(sql, tenantId));
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

    private Long count(String sql, Long tenantId) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class, tenantId);
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
        keyMap.put("materialid", "materialId");
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
        keyMap.put("grossmargin", "grossMargin");
        return keyMap.getOrDefault(key, key);
    }
}
