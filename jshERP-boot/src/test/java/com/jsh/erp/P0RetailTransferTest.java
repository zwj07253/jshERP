package com.jsh.erp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * P0: 零售闭环 + 调拨与仓库测试 (ID 15-20)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("P0: 零售与调拨")
public class P0RetailTransferTest extends ApiTestBase {

    private static Long materialExtendId;
    private static String materialBarCode;
    private static String materialUnit;
    private static Long depotId;
    private static Long depotId2;
    private static Long accountId;
    private static Long retailHeadId;

    @Test
    @Order(0)
    @DisplayName("准备: 查询基础数据")
    void prepareData() {
        Response matResp = authReqGet().param("q", "").param("page", 1).param("rows", 10).get(CONTEXT + "/material/findBySelect");
        JSONObject matData = JSONObject.parseObject(matResp.body().asString());
        JSONArray materialRows = matData.getJSONArray("rows");
        if (materialRows != null) {
            for (int index = 0; index < materialRows.size(); index++) {
                JSONObject material = materialRows.getJSONObject(index);
                if (!"1".equals(material.getString("enableSerialNumber"))
                        && !"1".equals(material.getString("enableBatchNumber"))) {
                    materialExtendId = material.getLong("id");
                    materialBarCode = material.getString("mBarCode");
                    materialUnit = material.getString("unit").replace("[基本]", "");
                    break;
                }
            }
        }

        Response depResp = authReqGet().param("search", "{}").get(CONTEXT + "/depot/list");
        JSONObject depData = JSONObject.parseObject(depResp.body().asString()).getJSONObject("data");
        if (depData.getLong("total") > 0) depotId = depData.getJSONArray("rows").getJSONObject(0).getLong("id");
        if (depData.getLong("total") > 1) depotId2 = depData.getJSONArray("rows").getJSONObject(1).getLong("id");

        Response accResp = authReqGet().param("search", "{}").get(CONTEXT + "/account/list");
        JSONObject accData = JSONObject.parseObject(accResp.body().asString()).getJSONObject("data");
        if (accData.getLong("total") > 0) accountId = accData.getJSONArray("rows").getJSONObject(0).getLong("id");
    }

    // ===== 15. 零售出库 =====

    @Test
    @Order(1)
    @DisplayName("15a: 创建零售出库单")
    void createRetailOutbound() {
        retailHeadId = createDepotHeadAndDetail("出库", "零售", null, accountId,
                depotId, materialExtendId, 2, 150.0);
        assertNotNull(retailHeadId, "零售出库单创建成功应返回ID");
    }

    @Test
    @Order(2)
    @DisplayName("15b: 审核零售出库单")
    void auditRetailOutbound() {
        auditDepotHead(retailHeadId);
        Response resp = authReqGet()
                .param("id", retailHeadId)
                .get(CONTEXT + "/depotHead/info");
        assertSuccess(resp);
    }

    // ===== 16. 零售退货 =====

    @Test
    @Order(3)
    @DisplayName("16: 创建零售退货单")
    void createRetailReturn() {
        Long returnHeadId = createDepotHeadAndDetail("入库", "零售退货", null, accountId,
                depotId, materialExtendId, 1, 150.0);
        assertNotNull(returnHeadId, "零售退货单创建成功应返回ID");
        auditDepotHead(returnHeadId);
    }

    // ===== 17. 会员余额 =====

    @Test
    @Order(4)
    @DisplayName("17: 查询零售客户（会员）列表")
    void memberBalance() {
        Response resp = authReqGet()
                .param("search", "{}")
                .get(CONTEXT + "/supplier/getAllCustomer");
        assertSuccess(resp);
    }

    // ===== 18. 调拨出库 =====

    @Test
    @Order(5)
    @DisplayName("18: 调拨单创建与审核")
    void transferDepot() {
        assumeTrue(materialExtendId != null && materialBarCode != null && materialUnit != null,
                "缺少非序列号、非批次调拨商品");
        if (depotId2 == null) {
            // 如果只有一个仓库，创建第二个
            depotId2 = createDepot("测试仓库B_" + System.currentTimeMillis());
        }
        assertNotNull(depotId2, "需要至少2个仓库");

        Long headId = null;
        boolean audited = false;
        try {
            String number = generateNumber("DBCK");
            JSONObject info = new JSONObject();
            info.put("number", number);
            info.put("type", "出库");
            info.put("subType", "调拨");
            info.put("totalPrice", 99999);
            info.put("status", 0);
            info.put("remark", "自动化测试调拨");

            JSONArray rows = new JSONArray();
            JSONObject item = new JSONObject();
            item.put("materialExtendId", materialExtendId);
            item.put("barCode", materialBarCode);
            item.put("unit", materialUnit);
            item.put("depotId", depotId);
            item.put("anotherDepotId", depotId2);
            item.put("operNumber", 1);
            item.put("unitPrice", 100);
            item.put("allPrice", 99999);
            item.put("taxRate", 99);
            item.put("taxMoney", 99999);
            rows.add(item);

            JSONObject body = new JSONObject();
            body.put("info", info.toJSONString());
            body.put("rows", rows.toJSONString());

            Response resp = authReq().body(body.toJSONString()).post(CONTEXT + "/depotHead/addDepotHeadAndDetail");
            assertSuccess(resp);

            Response listResp = authReqGet()
                    .param("search", "{\"number\":\"" + number + "\"}")
                    .get(CONTEXT + "/depotHead/list");
            JSONObject data = JSONObject.parseObject(listResp.body().asString()).getJSONObject("data");
            JSONArray listRows = data.getJSONArray("rows");
            assertNotNull(listRows);
            assertFalse(listRows.isEmpty());
            headId = listRows.getJSONObject(0).getLong("id");
            assertEquals(0, listRows.getJSONObject(0).getBigDecimal("totalPrice")
                    .compareTo(new java.math.BigDecimal("100.00")));
            auditDepotHead(headId);
            audited = true;
        } finally {
            if (headId != null) {
                if (audited) {
                    unauditDepotHead(headId);
                }
                assertSuccess(authReq().param("id", headId).delete(CONTEXT + "/depotHead/delete"));
            }
        }
    }

    // ===== 19. 其它入库/出库 =====

    @Test
    @Order(6)
    @DisplayName("19a: 其它入库")
    void otherInbound() {
        Long headId = createDepotHeadAndDetail("入库", "其它", null, accountId,
                depotId, materialExtendId, 5, 80.0);
        assertNotNull(headId, "其它入库单创建成功应返回ID");
        auditDepotHead(headId);
    }

    @Test
    @Order(7)
    @DisplayName("19b: 其它出库")
    void otherOutbound() {
        Long headId = createDepotHeadAndDetail("出库", "其它", null, accountId,
                depotId, materialExtendId, 2, 80.0);
        assertNotNull(headId, "其它出库单创建成功应返回ID");
        auditDepotHead(headId);
    }

    // ===== 20. 组装/拆卸 =====

    @Test
    @Order(8)
    @DisplayName("20: 组装/拆卸单据列表查询")
    void assemblyList() {
        // 查询所有单据类型验证接口可用
        Response resp = authReqGet()
                .param("search", "{\"subType\":\"组装单\"}")
                .get(CONTEXT + "/depotHead/list");
        assertPaged(resp);
        assertTrue(resp.jsonPath().getInt("data.total") > 0, "应查询到组装单初始化数据");

        Response resp2 = authReqGet()
                .param("search", "{\"subType\":\"拆卸单\"}")
                .get(CONTEXT + "/depotHead/list");
        assertPaged(resp2);
        assertTrue(resp2.jsonPath().getInt("data.total") > 0, "应查询到拆卸单初始化数据");
    }
}
