package com.jsh.erp;

import io.restassured.response.Response;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.math.BigDecimal;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * P1: 报表对账测试 (ID 30-40)
 * 前置条件：P0核心业务链路测试数据已存在
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("P1: 报表对账")
public class P1ReportTest extends ApiTestBase {

    private static final String BEGIN_TIME = "2020-01-01 00:00:00";
    private static final String END_TIME = "2030-12-31 23:59:59";

    // ===== 30. 商品库存报表 =====

    @Test
    @Order(1)
    @DisplayName("30a: 进销存报表 - 入库/出库/结存")
    void inventoryReport() {
        Response resp = authReqGet()
                .param("currentPage", 1)
                .param("pageSize", 10)
                .param("depotIds", "")
                .param("categoryId", "")
                .param("beginTime", BEGIN_TIME)
                .param("endTime", END_TIME)
                .param("materialParam", "")
                .param("mpList", "")
                .get(CONTEXT + "/depotItem/getInOutStock");
        assertPaged(resp);
    }

    @Test
    @Order(2)
    @DisplayName("30b: 库存总金额")
    void inventoryMoney() {
        Response resp = authReqGet()
                .param("depotIds", "")
                .param("categoryId", "")
                .param("endTime", END_TIME)
                .param("materialParam", "")
                .get(CONTEXT + "/depotItem/getInOutStockCountMoney");
        assertSuccess(resp);
    }

    // ===== 31. 采购统计 =====

    @Test
    @Order(3)
    @DisplayName("31: 采购统计报表")
    void purchaseStatistics() {
        Response resp = authReqGet()
                .param("currentPage", 1)
                .param("pageSize", 10)
                .param("beginTime", BEGIN_TIME)
                .param("endTime", END_TIME)
                .param("organId", "")
                .param("depotId", "")
                .param("categoryId", "")
                .param("organizationId", "")
                .param("materialParam", "")
                .param("mpList", "")
                .get(CONTEXT + "/depotItem/buyIn");
        assertPaged(resp);
    }

    // ===== 32. 销售统计 =====

    @Test
    @Order(4)
    @DisplayName("32: 销售统计报表")
    void salesStatistics() {
        Response resp = authReqGet()
                .param("currentPage", 1)
                .param("pageSize", 10)
                .param("beginTime", BEGIN_TIME)
                .param("endTime", END_TIME)
                .param("organId", "")
                .param("depotId", "")
                .param("categoryId", "")
                .param("organizationId", "")
                .param("materialParam", "")
                .param("mpList", "")
                .get(CONTEXT + "/depotItem/saleOut");
        assertPaged(resp);
    }

    // ===== 33. 零售统计 =====

    @Test
    @Order(5)
    @DisplayName("33: 零售统计报表")
    void retailStatistics() {
        Response resp = authReqGet()
                .param("currentPage", 1)
                .param("pageSize", 10000)
                .param("beginTime", BEGIN_TIME)
                .param("endTime", END_TIME)
                .param("organId", "")
                .param("depotId", "")
                .param("categoryId", "")
                .param("organizationId", "")
                .param("materialParam", "")
                .param("mpList", "")
                .get(CONTEXT + "/depotItem/retailOut");
        assertPaged(resp);
        JSONObject data = JSONObject.parseObject(resp.body().asString()).getJSONObject("data");
        JSONArray rows = data.getJSONArray("rows");
        BigDecimal rowNet = BigDecimal.ZERO;
        for (int i = 0; i < rows.size(); i++) {
            JSONObject row = rows.getJSONObject(i);
            BigDecimal out = row.getBigDecimal("outSumPrice");
            BigDecimal in = row.getBigDecimal("inSumPrice");
            BigDecimal net = row.getBigDecimal("outInSumPrice");
            assertEquals(0, out.subtract(in).compareTo(net), "零售行净额必须等于零售金额减退货金额");
            rowNet = rowNet.add(net);
        }
        assertEquals(0, rowNet.compareTo(data.getBigDecimal("realityPriceTotal")),
                "零售列表合计必须与顶部实际零售金额一致");
        assertEquals(rows.size(), data.getIntValue("total"), "分页总数必须与完整结果行数一致");
    }

    // ===== 34. 账户统计 =====

    @Test
    @Order(6)
    @DisplayName("34: 账户余额统计")
    void accountStatistics() {
        Response resp = authReqGet()
                .param("name", "")
                .param("serialNo", "")
                .get(CONTEXT + "/account/getStatistics");
        assertSuccess(resp);
    }

    // ===== 35. 客户对账 =====

    @Test
    @Order(7)
    @DisplayName("35: 客户对账报表")
    void customerReconciliation() {
        Response resp = authReqGet()
                .param("currentPage", 1)
                .param("pageSize", 10)
                .param("beginTime", BEGIN_TIME)
                .param("endTime", END_TIME)
                .param("organId", "")
                .param("hasDebt", "")
                .param("supplierType", "客户")
                .get(CONTEXT + "/depotHead/getStatementAccount");
        assertPaged(resp);
    }

    // ===== 36. 供应商对账 =====

    @Test
    @Order(8)
    @DisplayName("36: 供应商对账报表")
    void supplierReconciliation() {
        Response resp = authReqGet()
                .param("currentPage", 1)
                .param("pageSize", 10)
                .param("beginTime", BEGIN_TIME)
                .param("endTime", END_TIME)
                .param("organId", "")
                .param("hasDebt", "")
                .param("supplierType", "供应商")
                .get(CONTEXT + "/depotHead/getStatementAccount");
        assertPaged(resp);
    }

    // ===== 37. 进销存统计 =====

    @Test
    @Order(9)
    @DisplayName("37: 进销存统计 - 入库明细")
    void inOutDetail() {
        Response resp = authReqGet()
                .param("currentPage", 1)
                .param("pageSize", 10)
                .param("organId", "")
                .param("number", "")
                .param("materialParam", "")
                .param("depotId", "")
                .param("beginTime", BEGIN_TIME)
                .param("endTime", END_TIME)
                .param("type", "")
                .param("creator", "")
                .param("categoryId", "")
                .param("organizationId", "")
                .param("remark", "")
                .param("column", "")
                .param("order", "")
                .get(CONTEXT + "/depotHead/findInOutDetail");
        assertPaged(resp);
    }

    // ===== 38. 库存预警 =====

    @Test
    @Order(10)
    @DisplayName("38: 库存预警报表")
    void stockWarning() {
        Response resp = authReqGet()
                .param("currentPage", 1)
                .param("pageSize", 10)
                .param("materialParam", "")
                .param("depotId", "")
                .param("categoryId", "")
                .param("mpList", "")
                .get(CONTEXT + "/depotItem/findStockWarningCount");
        assertPaged(resp);
    }

    // ===== 39. 入库/出库/调拨明细 =====

    @Test
    @Order(11)
    @DisplayName("39a: 入库/出库明细 - 按商品汇总")
    void materialCount() {
        Response resp = authReqGet()
                .param("currentPage", 1)
                .param("pageSize", 10)
                .param("organId", "")
                .param("materialParam", "")
                .param("depotId", "")
                .param("categoryId", "")
                .param("organizationId", "")
                .param("beginTime", BEGIN_TIME)
                .param("endTime", END_TIME)
                .param("type", "")
                .param("column", "")
                .param("order", "")
                .get(CONTEXT + "/depotHead/findInOutMaterialCount");
        assertPaged(resp);
    }

    @Test
    @Order(12)
    @DisplayName("39b: 调拨明细")
    void allocationDetail() {
        Response resp = authReqGet()
                .param("currentPage", 1)
                .param("pageSize", 10)
                .param("number", "")
                .param("materialParam", "")
                .param("depotId", "")
                .param("depotIdF", "")
                .param("categoryId", "")
                .param("organizationId", "")
                .param("beginTime", BEGIN_TIME)
                .param("endTime", END_TIME)
                .param("subType", "调拨")
                .param("remark", "")
                .param("column", "")
                .param("order", "")
                .get(CONTEXT + "/depotHead/findAllocationDetail");
        assertPaged(resp);
    }

    // ===== 40. 入库/出库汇总 =====

    @Test
    @Order(13)
    @DisplayName("40: 购销统计汇总")
    void buyAndSaleSummary() {
        Response resp = authReqGet().get(CONTEXT + "/depotHead/getBuyAndSaleStatistics");
        assertSuccess(resp);
    }

    @Test
    @Order(14)
    @DisplayName("40b: 月度购销趋势")
    void monthlyTrend() {
        Response resp = authReqGet().get(CONTEXT + "/depotItem/buyOrSalePrice");
        assertSuccess(resp);
    }
}
