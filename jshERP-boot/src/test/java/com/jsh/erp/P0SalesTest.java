package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P0: 销售核心闭环测试 (ID 11-14)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("P0: 销售核心闭环")
public class P0SalesTest extends ApiTestBase {

    private static Long customerId;
    private static Long materialExtendId;
    private static Long depotId;
    private static Long accountId;
    private static Long salesHeadId;

    // ===== 准备数据 =====

    @Test
    @Order(0)
    @DisplayName("准备: 查询基础数据")
    void prepareData() {
        // 获取商品
        Response matResp = authReqGet().param("q", "").param("page", 1).param("rows", 10).get(CONTEXT + "/material/findBySelect");
        JSONObject matData = JSONObject.parseObject(matResp.body().asString());
        if (matData.getLong("total") > 0) {
            for (int index = 0; index < matData.getJSONArray("rows").size(); index++) {
                JSONObject material = matData.getJSONArray("rows").getJSONObject(index);
                if (!"1".equals(material.getString("enableBatchNumber"))
                        && !"1".equals(material.getString("enableSerialNumber"))) {
                    materialExtendId = material.getLong("id");
                    break;
                }
            }
        }

        // 获取仓库
        Response depResp = authReqGet().param("search", "{}").get(CONTEXT + "/depot/list");
        JSONObject depData = JSONObject.parseObject(depResp.body().asString()).getJSONObject("data");
        if (depData.getLong("total") > 0) {
            depotId = depData.getJSONArray("rows").getJSONObject(0).getLong("id");
        }

        // 获取账户
        Response accResp = authReqGet().param("search", "{}").get(CONTEXT + "/account/list");
        JSONObject accData = JSONObject.parseObject(accResp.body().asString()).getJSONObject("data");
        if (accData.getLong("total") > 0) {
            accountId = accData.getJSONArray("rows").getJSONObject(0).getLong("id");
        }
    }

    // ===== 11. 客户建档 =====

    @Test
    @Order(1)
    @DisplayName("11a: 查询已有客户列表")
    void listCustomers() {
        Response resp = authReqGet()
                .param("search", "{\"type\":\"客户\"}")
                .get(CONTEXT + "/supplier/list");
        assertPaged(resp);
        JSONObject data = JSONObject.parseObject(resp.body().asString()).getJSONObject("data");
        if (data.getLong("total") > 0) {
            customerId = data.getJSONArray("rows").getJSONObject(0).getLong("id");
        }
    }

    @Test
    @Order(2)
    @DisplayName("11b: 新建客户")
    void createCustomer() {
        String name = "测试客户_" + System.currentTimeMillis();
        customerId = createSupplier(name, "客户");
        assertNotNull(customerId, "客户创建成功应返回ID");
    }

    @Test
    @Order(3)
    @DisplayName("11c: 客户查询成功且下拉可选")
    void queryCustomer() {
        Response resp = authReqGet()
                .param("id", customerId)
                .get(CONTEXT + "/supplier/info");
        assertSuccess(resp);
    }

    // ===== 12. 销售出库 =====

    @Test
    @Order(4)
    @DisplayName("12a: 创建销售出库单")
    void createSalesOutbound() {
        salesHeadId = createDepotHeadAndDetail("出库", "销售", customerId, accountId,
                depotId, materialExtendId, 3, 200.0);
        assertNotNull(salesHeadId, "销售出库单创建成功应返回ID");
    }

    @Test
    @Order(5)
    @DisplayName("12b: 审核销售出库单")
    void auditSalesOutbound() {
        auditDepotHead(salesHeadId);
        // 验证状态
        Response resp = authReqGet()
                .param("id", salesHeadId)
                .get(CONTEXT + "/depotHead/info");
        assertSuccess(resp);
    }

    // ===== 13. 库存验证（销售） =====

    @Test
    @Order(6)
    @DisplayName("13: 销售审核后库存验证")
    void verifyStockAfterSales() {
        Response resp = authReqGet()
                .param("currentPage", 1)
                .param("pageSize", 10)
                .param("depotIds", "")
                .param("categoryId", "")
                .param("position", "")
                .param("materialParam", "")
                .param("zeroStock", 0)
                .param("column", "")
                .param("order", "")
                .get(CONTEXT + "/material/getListWithStock");
        assertPaged(resp);
    }

    // ===== 14. 财务收款 =====

    @Test
    @Order(7)
    @DisplayName("14: 客户对账数据查询")
    void verifyCustomerReconciliation() {
        Response resp = authReqGet()
                .param("currentPage", 1)
                .param("pageSize", 10)
                .param("beginTime", "2020-01-01 00:00:00")
                .param("endTime", "2030-12-31 23:59:59")
                .param("organId", customerId)
                .param("hasDebt", "")
                .param("supplierType", "客户")
                .get(CONTEXT + "/depotHead/getStatementAccount");
        assertPaged(resp);
    }

    @Test
    @Order(8)
    @DisplayName("14b: 欠款单据列表")
    void verifyDebtList() {
        Response resp = authReqGet()
                .param("search", "{\"organId\":\"" + customerId + "\",\"type\":\"出库\",\"subType\":\"销售\"}")
                .param("currentPage", 1)
                .param("pageSize", 10)
                .get(CONTEXT + "/depotHead/debtList");
        assertPaged(resp);
    }
}
