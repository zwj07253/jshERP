package com.jsh.erp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("P0: 拆卸单业务校验")
public class P0DisassembleValidationTest extends ApiTestBase {

    private static final List<JSONObject> materials = new ArrayList<>();
    private static Long depotId;

    @BeforeAll
    static void prepareDisassembleData() {
        P0DisassembleValidationTest test = new P0DisassembleValidationTest();
        Response depotResponse = test.authReqGet().param("search", "{}").get(CONTEXT + "/depot/list");
        JSONArray depots = JSONObject.parseObject(depotResponse.body().asString())
                .getJSONObject("data").getJSONArray("rows");
        if (depots != null && !depots.isEmpty()) {
            depotId = depots.getJSONObject(0).getLong("id");
        }

        Response materialResponse = test.authReqGet().param("q", "").param("page", 1).param("rows", 100)
                .get(CONTEXT + "/material/findBySelect");
        JSONArray materialRows = JSONObject.parseObject(materialResponse.body().asString()).getJSONArray("rows");
        if (materialRows != null && depotId != null) {
            for (int index = 0; index < materialRows.size() && materials.size() < 3; index++) {
                JSONObject material = materialRows.getJSONObject(index);
                if (!"1".equals(material.getString("enableBatchNumber"))
                        && !"1".equals(material.getString("enableSerialNumber"))) {
                    Response stockResponse = test.authReqGet().param("depotId", depotId)
                            .param("barCode", material.getString("mBarCode"))
                            .get(CONTEXT + "/depotItem/findStockByDepotAndBarCode");
                    BigDecimal stock = JSONObject.parseObject(stockResponse.body().asString())
                            .getJSONObject("data").getBigDecimal("stock");
                    if (stock != null && stock.compareTo(BigDecimal.ZERO) > 0) {
                        materials.add(material);
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("服务端按组合件成本分摊子件并支持审核回滚")
    void normalizeCostAndAudit() {
        assumeTrue(hasBaseData(), "缺少拆卸测试基础数据");
        String number = generateNumber("CXD");
        Long headId = null;
        boolean audited = false;
        try {
            Response response = submitDisassemble(number, "0", depotId, normalRows(false));
            assertSuccess(response);
            headId = getHeadId(number);

            JSONObject head = getHead(headId);
            JSONArray detailRows = getDetails(headId);
            assertEquals(3, detailRows.size());
            assertEquals("组合件", detailRows.getJSONObject(0).getString("mType"));
            assertEquals("普通子件", detailRows.getJSONObject(1).getString("mType"));
            assertEquals("普通子件", detailRows.getJSONObject(2).getString("mType"));

            BigDecimal combinationCost = detailRows.getJSONObject(0).getBigDecimal("allPrice");
            BigDecimal componentCost = detailRows.getJSONObject(1).getBigDecimal("allPrice")
                    .add(detailRows.getJSONObject(2).getBigDecimal("allPrice"));
            assertEquals(0, combinationCost.compareTo(componentCost));
            assertEquals(0, head.getBigDecimal("totalPrice").compareTo(combinationCost));
            assertNotEquals(0, combinationCost.compareTo(new BigDecimal("88888")));
            assertNull(head.getLong("accountId"));
            assertNull(head.getString("linkNumber"));

            auditDepotHead(headId);
            audited = true;
        } finally {
            if (headId != null) {
                if (audited) {
                    unauditDepotHead(headId);
                }
                deleteDraft(headId);
            }
        }
    }

    @Test
    @DisplayName("拒绝空明细、非法状态结构、同物料、越权仓库和组合件库存不足")
    void rejectInvalidDisassembly() {
        assumeTrue(hasBaseData(), "缺少拆卸测试基础数据");
        assertBizCode(submitDisassemble(generateNumber("CXD"), "0", depotId, new JSONArray()),
                ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_CODE);
        assertBizCode(submitDisassemble(generateNumber("CXD"), "3", depotId, normalRows(false)),
                ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STATUS_CODE);

        JSONArray invalidStructure = normalRows(false);
        invalidStructure.getJSONObject(0).put("mType", "普通子件");
        assertBizCode(submitDisassemble(generateNumber("CXD"), "0", depotId, invalidStructure),
                ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_CODE);

        assertBizCode(submitDisassemble(generateNumber("CXD"), "0", depotId, normalRows(true)),
                ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_SAME_MATERIAL_CODE);
        assertBizCode(submitDisassemble(generateNumber("CXD"), "0", 999999L, normalRows(false)),
                ExceptionConstants.DEPOT_DATA_PERMISSION_CODE);

        JSONArray overStock = normalRows(false);
        overStock.getJSONObject(0).put("operNumber", 999999);
        String overStockNumber = generateNumber("CXD");
        Response overStockResponse = submitDisassemble(overStockNumber, "0", depotId, overStock);
        JSONObject overStockBody = JSONObject.parseObject(overStockResponse.body().asString());
        if (overStockBody.getIntValue("code") == 200) {
            Long headId = getHeadId(overStockNumber);
            try {
                assertBizCode(setStatus(headId, "1"),
                        ExceptionConstants.BILL_MATERIAL_STOCK_NOT_ENOUGH_CODE);
            } finally {
                deleteDraft(headId);
            }
        } else {
            assertEquals(ExceptionConstants.BILL_MATERIAL_STOCK_NOT_ENOUGH_CODE,
                    overStockBody.getIntValue("code"), overStockResponse.body().asString());
        }
    }

    @Test
    @DisplayName("无菜单数据权限的账号不能按ID读取或调用新增接口")
    void enforceButtonAndDataPermission() {
        Response sample = authReqGet().param("id", 411).get(CONTEXT + "/depotHead/info");
        assumeTrue(JSONObject.parseObject(sample.body().asString()).getIntValue("code") == 200,
                "缺少拆卸初始化单据");
        String token = login("zhangwei", md5("123456"));
        assumeTrue(token != null, "缺少权限测试账号");

        Response infoResponse = RestAssured.given().header("X-Access-Token", token)
                .param("id", 411).get(CONTEXT + "/depotHead/info");
        assertBizCode(infoResponse, ExceptionConstants.DEPOT_DATA_PERMISSION_CODE);

        JSONObject requestBody = buildRequest(generateNumber("CXD"), "0", normalRows(false));
        Response addResponse = RestAssured.given().contentType(ContentType.JSON)
                .header("X-Access-Token", token).body(requestBody.toJSONString())
                .post(CONTEXT + "/depotHead/addDepotHeadAndDetail");
        assertBizCode(addResponse, ExceptionConstants.DEPOT_HEAD_RETAIL_PERMISSION_CODE);
    }

    private boolean hasBaseData() {
        return depotId != null && materials.size() >= 3;
    }

    private JSONArray normalRows(boolean sameCombinationAndComponent) {
        JSONArray rows = new JSONArray();
        rows.add(buildRow(materials.get(0), "组合件", 1, depotId));
        rows.add(buildRow(sameCombinationAndComponent ? materials.get(0) : materials.get(1),
                "普通子件", 1, depotId));
        rows.add(buildRow(materials.get(2), "普通子件", 2, depotId));
        return rows;
    }

    private JSONObject buildRow(JSONObject material, String materialType, double quantity, Long rowDepotId) {
        JSONObject row = new JSONObject();
        row.put("materialExtendId", material.getLong("id"));
        row.put("barCode", material.getString("mBarCode"));
        row.put("unit", material.getString("unit").replace("[基本]", ""));
        row.put("depotId", rowDepotId);
        row.put("mType", materialType);
        row.put("operNumber", quantity);
        row.put("unitPrice", 99999);
        row.put("allPrice", 88888);
        row.put("taxRate", 99);
        row.put("taxMoney", 77777);
        row.put("taxLastMoney", 66666);
        row.put("linkId", 999999);
        return row;
    }

    private Response submitDisassemble(String number, String status, Long rowDepotId, JSONArray rows) {
        for (int index = 0; index < rows.size(); index++) {
            rows.getJSONObject(index).put("depotId", rowDepotId);
        }
        return authReq().body(buildRequest(number, status, rows).toJSONString())
                .post(CONTEXT + "/depotHead/addDepotHeadAndDetail");
    }

    private JSONObject buildRequest(String number, String status, JSONArray rows) {
        JSONObject info = new JSONObject();
        info.put("number", number);
        info.put("type", "其它");
        info.put("subType", "拆卸单");
        info.put("status", status);
        info.put("totalPrice", 999999);
        info.put("accountId", 999999);
        info.put("organId", 999999);
        info.put("linkNumber", "FORGED-LINK");
        info.put("changeAmount", 999999);
        JSONObject body = new JSONObject();
        body.put("info", info.toJSONString());
        body.put("rows", rows.toJSONString());
        return body;
    }

    private Long getHeadId(String number) {
        Response response = authReqGet().param("number", number)
                .get(CONTEXT + "/depotHead/getDetailByNumber");
        assertSuccess(response);
        return JSONObject.parseObject(response.body().asString()).getJSONObject("data").getLong("id");
    }

    private JSONObject getHead(Long headId) {
        Response response = authReqGet().param("id", headId).get(CONTEXT + "/depotHead/info");
        assertSuccess(response);
        return JSONObject.parseObject(response.body().asString()).getJSONObject("data").getJSONObject("info");
    }

    private JSONArray getDetails(Long headId) {
        Response response = authReqGet().param("headerId", headId).param("linkType", "basic")
                .get(CONTEXT + "/depotItem/getDetailList");
        assertSuccess(response);
        return JSONObject.parseObject(response.body().asString()).getJSONObject("data").getJSONArray("rows");
    }

    private void deleteDraft(Long headId) {
        assertSuccess(authReq().param("id", headId).delete(CONTEXT + "/depotHead/delete"));
    }

    private Response setStatus(Long headId, String status) {
        JSONObject body = new JSONObject();
        body.put("status", status);
        body.put("ids", String.valueOf(headId));
        return authReq().body(body.toJSONString()).post(CONTEXT + "/depotHead/batchSetStatus");
    }

    private void assertBizCode(Response response, int expectedCode) {
        assertBizError(response);
        assertEquals(expectedCode, JSONObject.parseObject(response.body().asString()).getIntValue("code"),
                response.body().asString());
    }
}
