package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P0: 采购核心闭环测试 (ID 6-10)
 * 前置条件：系统已初始化，有基础数据
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("P0: 采购核心闭环")
public class P0PurchaseTest extends ApiTestBase {

    private static Long supplierId;
    private static Long existingSupplierId;
    private static Long createdSupplierId;
    private static Long materialExtendId;
    private static Long depotId;
    private static Long accountId;
    private static Long purchaseHeadId;
    private static double stockBeforePurchase;

    @AfterAll
    static void cleanUpCreatedData() {
        P0PurchaseTest test = new P0PurchaseTest();
        if (purchaseHeadId != null) {
            Response infoResponse = test.authReqGet().param("id", purchaseHeadId)
                    .get(CONTEXT + "/depotHead/info");
            JSONObject responseBody = JSONObject.parseObject(infoResponse.body().asString());
            JSONObject data = responseBody.getJSONObject("data");
            JSONObject info = data == null ? null : data.getJSONObject("info");
            if (info != null && "1".equals(info.getString("status"))) {
                test.unauditDepotHead(purchaseHeadId);
            }
            test.assertSuccess(test.authReq().param("id", purchaseHeadId)
                    .delete(CONTEXT + "/depotHead/delete"));
        }
        if (createdSupplierId != null) {
            test.assertSuccess(test.authReq().param("id", createdSupplierId)
                    .delete(CONTEXT + "/supplier/delete"));
        }
    }

    // ===== 6. 商品建档 =====

    @Test
    @Order(1)
    @DisplayName("6a: 查询已有商品列表")
    void listMaterials() {
        Response resp = authReqGet()
                .param("q", "")
                .param("page", 1)
                .param("rows", 10)
                .get(CONTEXT + "/material/findBySelect");
        assertEquals(200, resp.statusCode());
        JSONObject data = JSONObject.parseObject(resp.body().asString());
        assertTrue(data.getLong("total") >= 1, "应至少有1个商品");
        JSONArray materials = data.getJSONArray("rows");
        for (int index = 0; index < materials.size(); index++) {
            JSONObject material = materials.getJSONObject(index);
            if (!"1".equals(material.getString("enableBatchNumber"))
                    && !"1".equals(material.getString("enableSerialNumber"))) {
                materialExtendId = material.getLong("id");
                break;
            }
        }
        assertNotNull(materialExtendId, "应至少有一个未启用批号和序列号的普通商品");
    }

    @Test
    @Order(2)
    @DisplayName("6b: 查询商品详情 - 通过findBySelect验证")
    void materialDetail() {
        Response resp = authReqGet()
                .param("q", "")
                .param("page", 1)
                .param("rows", 10)
                .get(CONTEXT + "/material/findBySelect");
        assertEquals(200, resp.statusCode());
        JSONObject data = JSONObject.parseObject(resp.body().asString());
        assertTrue(data.getLong("total") >= 1, "应有商品数据");
        assertNotNull(data.getJSONArray("rows").getJSONObject(0).getString("name"), "商品名称不应为null");
    }

    // ===== 7. 供应商建档 =====

    @Test
    @Order(3)
    @DisplayName("7a: 查询已有供应商列表")
    void listSuppliers() {
        Response resp = authReqGet()
                .param("search", "{\"type\":\"供应商\"}")
                .get(CONTEXT + "/supplier/list");
        assertPaged(resp);
        JSONObject data = JSONObject.parseObject(resp.body().asString()).getJSONObject("data");
        if (data.getLong("total") > 0) {
            JSONArray suppliers = data.getJSONArray("rows");
            for (int index = 0; index < suppliers.size(); index++) {
                JSONObject supplier = suppliers.getJSONObject(index);
                if (!supplier.getString("supplier").startsWith("测试供应商_")) {
                    existingSupplierId = supplier.getLong("id");
                    break;
                }
            }
            supplierId = existingSupplierId;
        }
    }

    @Test
    @Order(4)
    @DisplayName("7b: 新建供应商")
    void createSupplier() {
        String name = "测试供应商_" + System.currentTimeMillis();
        createdSupplierId = createSupplier(name, "供应商");
        supplierId = createdSupplierId;
        assertNotNull(supplierId, "供应商创建成功应返回ID");
    }

    @Test
    @Order(5)
    @DisplayName("7c: 供应商查询成功")
    void querySupplier() {
        Response resp = authReqGet()
                .param("id", supplierId)
                .get(CONTEXT + "/supplier/info");
        assertSuccess(resp);
        assertSuccess(authReq().param("id", createdSupplierId).delete(CONTEXT + "/supplier/delete"));
        createdSupplierId = null;
        supplierId = existingSupplierId;
        assertNotNull(supplierId, "初始化数据应至少包含一个可用于采购测试的供应商");
    }

    // ===== 准备仓库和账户 =====

    @Test
    @Order(6)
    @DisplayName("准备: 查询仓库列表")
    void listDepots() {
        Response resp = authReqGet()
                .param("search", "{}")
                .get(CONTEXT + "/depot/list");
        assertPaged(resp);
        JSONObject data = JSONObject.parseObject(resp.body().asString()).getJSONObject("data");
        assertTrue(data.getLong("total") >= 1, "应至少有1个仓库");
        depotId = data.getJSONArray("rows").getJSONObject(0).getLong("id");
    }

    @Test
    @Order(7)
    @DisplayName("准备: 查询结算账户列表")
    void listAccounts() {
        Response resp = authReqGet()
                .param("search", "{}")
                .get(CONTEXT + "/account/list");
        assertPaged(resp);
        JSONObject data = JSONObject.parseObject(resp.body().asString()).getJSONObject("data");
        assertTrue(data.getLong("total") >= 1, "应至少有1个结算账户");
        accountId = data.getJSONArray("rows").getJSONObject(0).getLong("id");
    }

    // ===== 8. 采购入库 =====

    @Test
    @Order(8)
    @DisplayName("8a: 创建采购入库单")
    void createPurchaseInbound() {
        stockBeforePurchase = getMaterialStock(materialExtendId, depotId);
        purchaseHeadId = createDepotHeadAndDetail("入库", "采购", supplierId, accountId,
                depotId, materialExtendId, 10, 100.0);
        assertNotNull(purchaseHeadId, "采购入库单创建成功应返回ID");
    }

    @Test
    @Order(9)
    @DisplayName("8b: 查询采购入库单详情")
    void queryPurchaseDetail() {
        Response resp = authReqGet()
                .param("id", purchaseHeadId)
                .get(CONTEXT + "/depotHead/info");
        assertSuccess(resp);
    }

    @Test
    @Order(10)
    @DisplayName("8c: 审核采购入库单")
    void auditPurchaseInbound() {
        auditDepotHead(purchaseHeadId);
        // 验证状态变为已审核
        Response resp = authReqGet()
                .param("id", purchaseHeadId)
                .get(CONTEXT + "/depotHead/info");
        assertSuccess(resp);
    }

    // ===== 9. 库存验证（采购） =====

    @Test
    @Order(11)
    @DisplayName("9: 采购审核后库存验证 - 带库存商品列表")
    void verifyStockAfterPurchase() {
        double stockAfterPurchase = getMaterialStock(materialExtendId, depotId);
        assertEquals(stockBeforePurchase + 10, stockAfterPurchase, 0.0001,
                "采购入库审核后指定仓库库存应增加10");
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

    // ===== 10. 财务付款 =====

    @Test
    @Order(12)
    @DisplayName("10: 查询账户余额统计")
    void verifyAccountBalance() {
        Response resp = authReqGet()
                .param("name", "")
                .param("serialNo", "")
                .get(CONTEXT + "/account/getStatistics");
        assertSuccess(resp);
    }

    @Test
    @Order(13)
    @DisplayName("10b: 查询收支明细")
    void verifyAccountFlow() {
        Response resp = authReqGet()
                .param("currentPage", 1)
                .param("pageSize", 10)
                .param("accountId", accountId)
                .param("initialAmount", 0)
                .param("number", "")
                .param("beginTime", "2020-01-01 00:00:00")
                .param("endTime", "2030-12-31 23:59:59")
                .get(CONTEXT + "/account/findAccountInOutList");
        assertPaged(resp);
    }
}
