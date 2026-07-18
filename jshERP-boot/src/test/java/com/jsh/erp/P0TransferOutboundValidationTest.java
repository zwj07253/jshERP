package com.jsh.erp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("P0: 调拨出库业务校验")
public class P0TransferOutboundValidationTest extends ApiTestBase {

    private static Long materialExtendId;
    private static String barCode;
    private static String unit;
    private static Long sourceDepotId;
    private static Long targetDepotId;

    @BeforeAll
    static void prepareTransferData() {
        P0TransferOutboundValidationTest test = new P0TransferOutboundValidationTest();
        Response materialResponse = test.authReqGet().param("q", "").param("page", 1).param("rows", 100)
                .get(CONTEXT + "/material/findBySelect");
        JSONArray materials = JSONObject.parseObject(materialResponse.body().asString()).getJSONArray("rows");
        if (materials != null) {
            for (int index = 0; index < materials.size(); index++) {
                JSONObject material = materials.getJSONObject(index);
                if (!"1".equals(material.getString("enableBatchNumber"))
                        && !"1".equals(material.getString("enableSerialNumber"))) {
                    materialExtendId = material.getLong("id");
                    barCode = material.getString("mBarCode");
                    unit = material.getString("unit").replace("[基本]", "");
                    break;
                }
            }
        }
        Response depotResponse = test.authReqGet().param("search", "{}").get(CONTEXT + "/depot/list");
        JSONArray depots = JSONObject.parseObject(depotResponse.body().asString())
                .getJSONObject("data").getJSONArray("rows");
        if (depots != null && depots.size() >= 2) {
            sourceDepotId = depots.getJSONObject(0).getLong("id");
            targetDepotId = depots.getJSONObject(1).getLong("id");
        }
    }

    @Test
    @DisplayName("服务端重算调拨金额、清空无关字段并支持审核")
    void normalizeTransferOutbound() {
        assumeTrue(hasBaseData(), "缺少调拨测试基础数据");
        String number = generateNumber("DBCK");
        Long headId = null;
        boolean audited = false;
        try {
            Response response = submitTransfer(number, "0", sourceDepotId, targetDepotId,
                    2, 12.345, 99999);
            assertSuccess(response);
            headId = getHeadId(number);
            JSONObject head = getRawHead(headId);
            assertEquals(0, head.getBigDecimal("totalPrice").compareTo(new BigDecimal("24.69")));
            assertEquals(0, head.getBigDecimal("changeAmount").compareTo(BigDecimal.ZERO));
            assertEquals(0, head.getBigDecimal("discountLastMoney").compareTo(BigDecimal.ZERO));
            assertNull(head.getString("linkNumber"));
            assertNull(head.getLong("accountId"));

            JSONObject detail = getFirstDetail(headId);
            assertEquals(0, detail.getBigDecimal("unitPrice").compareTo(new BigDecimal("12.345")));
            assertEquals(0, detail.getBigDecimal("allPrice").compareTo(new BigDecimal("24.69")));
            assertEquals(0, detail.getBigDecimal("taxRate").compareTo(BigDecimal.ZERO));
            assertEquals(targetDepotId, detail.getLong("anotherDepotId"));
            assertNull(detail.getLong("linkId"));

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
    @DisplayName("拒绝伪造状态、负单价、同仓调拨和无权限仓库")
    void rejectInvalidTransferFields() {
        assumeTrue(hasBaseData(), "缺少调拨测试基础数据");
        assertBizCode(submitTransfer(generateNumber("DBCK"), "3", sourceDepotId, targetDepotId,
                1, 10, 10), ExceptionConstants.DEPOT_HEAD_TRANSFER_STATUS_CODE);
        assertBizCode(submitTransfer(generateNumber("DBCK"), "0", sourceDepotId, targetDepotId,
                1, -10, -10), ExceptionConstants.DEPOT_HEAD_TRANSFER_AMOUNT_CODE);
        assertBizCode(submitTransfer(generateNumber("DBCK"), "0", sourceDepotId, sourceDepotId,
                1, 10, 10), ExceptionConstants.DEPOT_HEAD_ANOTHER_DEPOT_EQUAL_FAILED_CODE);
        assertBizCode(submitTransfer(generateNumber("DBCK"), "0", sourceDepotId, 999999L,
                1, 10, 10), ExceptionConstants.DEPOT_DATA_PERMISSION_CODE);
    }

    private boolean hasBaseData() {
        return materialExtendId != null && barCode != null && unit != null
                && sourceDepotId != null && targetDepotId != null;
    }

    private Response submitTransfer(String number, String status, Long depotId, Long anotherDepotId,
                                    double quantity, double unitPrice, double allPrice) {
        JSONObject info = new JSONObject();
        info.put("number", number);
        info.put("type", "出库");
        info.put("subType", "调拨");
        info.put("totalPrice", 99999);
        info.put("changeAmount", 88888);
        info.put("discountLastMoney", 77777);
        info.put("otherMoney", 66666);
        info.put("debt", 55555);
        info.put("accountId", 999999);
        info.put("linkNumber", "FORGED-LINK");
        info.put("status", status);

        JSONObject item = new JSONObject();
        item.put("materialExtendId", materialExtendId);
        item.put("barCode", barCode);
        item.put("unit", unit);
        item.put("depotId", depotId);
        item.put("anotherDepotId", anotherDepotId);
        item.put("operNumber", quantity);
        item.put("unitPrice", unitPrice);
        item.put("allPrice", allPrice);
        item.put("taxRate", 99);
        item.put("taxMoney", 99999);
        item.put("taxLastMoney", 99999);
        item.put("linkId", 999999);

        JSONArray rows = new JSONArray();
        rows.add(item);
        JSONObject body = new JSONObject();
        body.put("info", info.toJSONString());
        body.put("rows", rows.toJSONString());
        return authReq().body(body.toJSONString()).post(CONTEXT + "/depotHead/addDepotHeadAndDetail");
    }

    private void assertBizCode(Response response, int expectedCode) {
        assertBizError(response);
        assertEquals(expectedCode, JSONObject.parseObject(response.body().asString()).getIntValue("code"));
    }

    private Long getHeadId(String number) {
        Response response = authReqGet().param("number", number)
                .get(CONTEXT + "/depotHead/getDetailByNumber");
        assertSuccess(response);
        return JSONObject.parseObject(response.body().asString()).getJSONObject("data").getLong("id");
    }

    private JSONObject getRawHead(Long headId) {
        Response response = authReqGet().param("id", headId).get(CONTEXT + "/depotHead/info");
        assertSuccess(response);
        return JSONObject.parseObject(response.body().asString()).getJSONObject("data").getJSONObject("info");
    }

    private JSONObject getFirstDetail(Long headId) {
        Response response = authReqGet().param("headerId", headId).param("mpList", "")
                .param("linkType", "basic").get(CONTEXT + "/depotItem/getDetailList");
        assertSuccess(response);
        return JSONObject.parseObject(response.body().asString()).getJSONObject("data")
                .getJSONArray("rows").getJSONObject(0);
    }

    private void deleteDraft(Long headId) {
        assertSuccess(authReq().param("id", headId).delete(CONTEXT + "/depotHead/delete"));
    }
}
