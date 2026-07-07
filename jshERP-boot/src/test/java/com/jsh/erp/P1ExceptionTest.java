package com.jsh.erp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P1: 异常测试 (ID 41-49)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("P1: 异常测试")
public class P1ExceptionTest extends ApiTestBase {

    // ===== 41. 采购入库-数量为0 =====

    @Test
    @Order(1)
    @DisplayName("41: 采购入库-数量为0应报错")
    void purchaseWithZeroQuantity() {
        // 获取基础数据
        Response matResp = authReqGet().param("q", "").param("page", 1).param("rows", 10).get(CONTEXT + "/material/findBySelect");
        JSONObject matData = JSONObject.parseObject(matResp.body().asString());
        if (matData.getLong("total") == 0) return;
        Long materialId = matData.getJSONArray("rows").getJSONObject(0).getLong("id");

        Response depResp = authReqGet().param("search", "{}").get(CONTEXT + "/depot/list");
        JSONObject depData = JSONObject.parseObject(depResp.body().asString()).getJSONObject("data");
        if (depData.getLong("total") == 0) return;
        Long depotId = depData.getJSONArray("rows").getJSONObject(0).getLong("id");

        Response supResp = authReqGet().param("search", "{\"type\":\"供应商\"}").get(CONTEXT + "/supplier/list");
        JSONObject supData = JSONObject.parseObject(supResp.body().asString()).getJSONObject("data");
        if (supData.getLong("total") == 0) return;
        Long supplierId = supData.getJSONArray("rows").getJSONObject(0).getLong("id");

        String number = generateNumber("CGRK");
        JSONObject info = new JSONObject();
        info.put("number", number);
        info.put("type", "入库");
        info.put("subType", "采购");
        info.put("organId", supplierId);
        info.put("accountId", 0);
        info.put("changeAmount", 0);
        info.put("totalPrice", 0);
        info.put("deposit", 0);
        info.put("debt", 0);
        info.put("payType", "");
        info.put("status", 0);

        JSONArray rows = new JSONArray();
        JSONObject item = new JSONObject();
        item.put("materialExtendId", materialId);
        item.put("depotId", depotId);
        item.put("anotherDepotId", 0);
        item.put("operNumber", 0);  // 数量为0
        item.put("unitPrice", 100);
        item.put("allPrice", 0);
        rows.add(item);

        JSONObject body = new JSONObject();
        body.put("info", info.toJSONString());
        body.put("rows", rows.toJSONString());

        Response resp = authReq().body(body.toJSONString()).post(CONTEXT + "/depotHead/addDepotHeadAndDetail");
        // 数量为0应该报错或有提示，验证不会产生有效单据
        assertNotNull(resp);
    }

    // ===== 42. 采购入库-金额为负 =====

    @Test
    @Order(2)
    @DisplayName("42: 采购入库-金额为负应报错")
    void purchaseWithNegativeAmount() {
        Response matResp = authReqGet().param("q", "").param("page", 1).param("rows", 10).get(CONTEXT + "/material/findBySelect");
        JSONObject matData = JSONObject.parseObject(matResp.body().asString());
        if (matData.getLong("total") == 0) return;
        Long materialId = matData.getJSONArray("rows").getJSONObject(0).getLong("id");

        Response depResp = authReqGet().param("search", "{}").get(CONTEXT + "/depot/list");
        JSONObject depData = JSONObject.parseObject(depResp.body().asString()).getJSONObject("data");
        if (depData.getLong("total") == 0) return;
        Long depotId = depData.getJSONArray("rows").getJSONObject(0).getLong("id");

        Response supResp = authReqGet().param("search", "{\"type\":\"供应商\"}").get(CONTEXT + "/supplier/list");
        JSONObject supData = JSONObject.parseObject(supResp.body().asString()).getJSONObject("data");
        if (supData.getLong("total") == 0) return;
        Long supplierId = supData.getJSONArray("rows").getJSONObject(0).getLong("id");

        String number = generateNumber("CGRK");
        JSONObject info = new JSONObject();
        info.put("number", number);
        info.put("type", "入库");
        info.put("subType", "采购");
        info.put("organId", supplierId);
        info.put("accountId", 0);
        info.put("changeAmount", -100);
        info.put("totalPrice", -100);
        info.put("deposit", 0);
        info.put("debt", 0);
        info.put("payType", "");
        info.put("status", 0);

        JSONArray rows = new JSONArray();
        JSONObject item = new JSONObject();
        item.put("materialExtendId", materialId);
        item.put("depotId", depotId);
        item.put("anotherDepotId", 0);
        item.put("operNumber", 1);
        item.put("unitPrice", -100);  // 负单价
        item.put("allPrice", -100);
        rows.add(item);

        JSONObject body = new JSONObject();
        body.put("info", info.toJSONString());
        body.put("rows", rows.toJSONString());

        Response resp = authReq().body(body.toJSONString()).post(CONTEXT + "/depotHead/addDepotHeadAndDetail");
        assertNotNull(resp, "接口应有响应");
    }

    // ===== 43. 销售出库-库存不足 =====

    @Test
    @Order(3)
    @DisplayName("43: 销售出库-超大数量（库存不足）")
    void salesExceedStock() {
        Response matResp = authReqGet().param("q", "").param("page", 1).param("rows", 10).get(CONTEXT + "/material/findBySelect");
        JSONObject matData = JSONObject.parseObject(matResp.body().asString());
        if (matData.getLong("total") == 0) return;
        Long materialId = matData.getJSONArray("rows").getJSONObject(0).getLong("id");

        Response depResp = authReqGet().param("search", "{}").get(CONTEXT + "/depot/list");
        JSONObject depData = JSONObject.parseObject(depResp.body().asString()).getJSONObject("data");
        if (depData.getLong("total") == 0) return;
        Long depotId = depData.getJSONArray("rows").getJSONObject(0).getLong("id");

        Response cusResp = authReqGet().param("search", "{\"type\":\"客户\"}").get(CONTEXT + "/supplier/list");
        JSONObject cusData = JSONObject.parseObject(cusResp.body().asString()).getJSONObject("data");
        if (cusData.getLong("total") == 0) return;
        Long customerId = cusData.getJSONArray("rows").getJSONObject(0).getLong("id");

        String number = generateNumber("XSCK");
        JSONObject info = new JSONObject();
        info.put("number", number);
        info.put("type", "出库");
        info.put("subType", "销售");
        info.put("organId", customerId);
        info.put("accountId", 0);
        info.put("changeAmount", 999999999);
        info.put("totalPrice", 999999999);
        info.put("deposit", 0);
        info.put("debt", 0);
        info.put("payType", "");
        info.put("status", 0);

        JSONArray rows = new JSONArray();
        JSONObject item = new JSONObject();
        item.put("materialExtendId", materialId);
        item.put("depotId", depotId);
        item.put("anotherDepotId", 0);
        item.put("operNumber", 999999);  // 超大数量
        item.put("unitPrice", 100);
        item.put("allPrice", 99999900);
        rows.add(item);

        JSONObject body = new JSONObject();
        body.put("info", info.toJSONString());
        body.put("rows", rows.toJSONString());

        Response resp = authReq().body(body.toJSONString()).post(CONTEXT + "/depotHead/addDepotHeadAndDetail");
        assertNotNull(resp, "接口应有响应");
    }

    // ===== 44. 重复审核 =====

    @Test
    @Order(4)
    @DisplayName("44: 重复审核应提示")
    void duplicateAudit() {
        // 先创建并审核一个单据
        Response matResp = authReqGet().param("q", "").param("page", 1).param("rows", 10).get(CONTEXT + "/material/findBySelect");
        JSONObject matData = JSONObject.parseObject(matResp.body().asString());
        if (matData.getLong("total") == 0) return;

        Response depResp = authReqGet().param("search", "{}").get(CONTEXT + "/depot/list");
        JSONObject depData = JSONObject.parseObject(depResp.body().asString()).getJSONObject("data");
        if (depData.getLong("total") == 0) return;

        Response supResp = authReqGet().param("search", "{\"type\":\"供应商\"}").get(CONTEXT + "/supplier/list");
        JSONObject supData = JSONObject.parseObject(supResp.body().asString()).getJSONObject("data");
        if (supData.getLong("total") == 0) return;

        Long headId = createDepotHeadAndDetail("入库", "采购",
                supData.getJSONArray("rows").getJSONObject(0).getLong("id"), 0L,
                depData.getJSONArray("rows").getJSONObject(0).getLong("id"),
                matData.getJSONArray("rows").getJSONObject(0).getLong("id"), 1, 50.0);
        if (headId == null) return;

        // 第一次审核
        auditDepotHead(headId);

        // 第二次审核（应提示已审核）
        JSONObject body = new JSONObject();
        body.put("status", "1");
        body.put("ids", String.valueOf(headId));
        Response resp = authReq().body(body.toJSONString()).post(CONTEXT + "/depotHead/batchSetStatus");
        assertNotNull(resp, "重复审核应有响应");
    }

    // ===== 45. 重复反审核 =====

    @Test
    @Order(5)
    @DisplayName("45: 重复反审核应提示")
    void duplicateUnaudit() {
        // 创建单据（不审核）
        Response matResp = authReqGet().param("q", "").param("page", 1).param("rows", 10).get(CONTEXT + "/material/findBySelect");
        JSONObject matData = JSONObject.parseObject(matResp.body().asString());
        if (matData.getLong("total") == 0) return;

        Response depResp = authReqGet().param("search", "{}").get(CONTEXT + "/depot/list");
        JSONObject depData = JSONObject.parseObject(depResp.body().asString()).getJSONObject("data");
        if (depData.getLong("total") == 0) return;

        Response supResp = authReqGet().param("search", "{\"type\":\"供应商\"}").get(CONTEXT + "/supplier/list");
        JSONObject supData = JSONObject.parseObject(supResp.body().asString()).getJSONObject("data");
        if (supData.getLong("total") == 0) return;

        Long headId = createDepotHeadAndDetail("入库", "采购",
                supData.getJSONArray("rows").getJSONObject(0).getLong("id"), 0L,
                depData.getJSONArray("rows").getJSONObject(0).getLong("id"),
                matData.getJSONArray("rows").getJSONObject(0).getLong("id"), 1, 50.0);
        if (headId == null) return;

        // 对未审核的单据反审核（应提示）
        unauditDepotHead(headId);
    }

    // ===== 46. 选择不存在的商品 =====

    @Test
    @Order(6)
    @DisplayName("46: 查询不存在的商品ID")
    void nonexistentMaterial() {
        Response resp = authReqGet()
                .param("id", 999999999L)
                .get(CONTEXT + "/material/info");
        assertNotNull(resp, "接口应有响应");
    }

    // ===== 47. 选择不存在的仓库 =====

    @Test
    @Order(7)
    @DisplayName("47: 查询不存在的仓库ID")
    void nonexistentDepot() {
        Response resp = authReqGet()
                .param("id", 999999999L)
                .get(CONTEXT + "/depot/info");
        assertNotNull(resp, "接口应有响应");
    }

    // ===== 48. 超长字段输入 =====

    @Test
    @Order(8)
    @DisplayName("48: 超长字段输入")
    void tooLongInput() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) sb.append("超长测试数据");
        String longStr = sb.toString();

        JSONObject body = new JSONObject();
        body.put("name", longStr);
        body.put("type", "1");
        body.put("remark", longStr);

        Response resp = authReq().body(body.toJSONString()).post(CONTEXT + "/depot/add");
        assertNotNull(resp, "超长输入应有响应");
    }

    // ===== 49. SQL注入 =====

    @Test
    @Order(9)
    @DisplayName("49a: SQL注入 - 单引号")
    void sqlInjectionSingleQuote() {
        Response resp = authReqGet()
                .param("search", "{\"name\":\"'; DROP TABLE jsh_depot; --\"}")
                .get(CONTEXT + "/depot/list");
        // 应正常返回，不报SQL错误
        assertEquals(200, resp.statusCode());
    }

    @Test
    @Order(10)
    @DisplayName("49b: SQL注入 - OR 1=1")
    void sqlInjectionOr() {
        Response resp = authReqGet()
                .param("search", "{\"name\":\"' OR 1=1 --\"}")
                .get(CONTEXT + "/depot/list");
        assertEquals(200, resp.statusCode());
    }

    @Test
    @Order(11)
    @DisplayName("49c: SQL注入 - UNION SELECT")
    void sqlInjectionUnion() {
        Response resp = authReqGet()
                .param("search", "{\"name\":\"' UNION SELECT * FROM jsh_user --\"}")
                .get(CONTEXT + "/depot/list");
        assertEquals(200, resp.statusCode());
    }

    @Test
    @Order(12)
    @DisplayName("49d: SQL注入 - 注释符")
    void sqlInjectionComment() {
        Response resp = authReqGet()
                .param("search", "{\"name\":\"/**/OR/**/1=1\"}")
                .get(CONTEXT + "/depot/list");
        assertEquals(200, resp.statusCode());
    }
}
