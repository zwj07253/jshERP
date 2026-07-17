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

@DisplayName("P0: 其它出库业务校验")
public class P0OtherOutboundValidationTest extends ApiTestBase {

    private static String barCode;
    private static String unit;
    private static Long materialExtendId;
    private static Long depotId;
    private static Long supplierId;
    private static Long accountId;

    @BeforeAll
    static void prepareOtherOutboundData() {
        P0OtherOutboundValidationTest test = new P0OtherOutboundValidationTest();
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
        depotId = test.getFirstId(depotResponse);
        Response supplierResponse = test.authReqGet().param("search", "{\"type\":\"供应商\"}")
                .get(CONTEXT + "/supplier/list");
        supplierId = test.getFirstId(supplierResponse);
        Response accountResponse = test.authReqGet().param("search", "{}").get(CONTEXT + "/account/list");
        accountId = test.getFirstId(accountResponse);
    }

    @Test
    @DisplayName("独立其它出库由服务端重算金额并清空关联字段")
    void normalizeStandaloneOtherOutbound() {
        assumeTrue(hasBaseData(), "缺少其它出库测试基础数据");
        String number = generateNumber("QTCK");
        Long headId = null;
        try {
            Response response = submitOtherOutbound(number, "0", null, 999999L,
                    barCode, unit, depotId, 2, 12.345, 99999);
            assertSuccess(response);
            headId = getHeadId(number);
            JSONObject head = getRawHead(headId);
            assertEquals(0, head.getBigDecimal("totalPrice").compareTo(new BigDecimal("24.69")));
            assertEquals(0, head.getBigDecimal("changeAmount").compareTo(BigDecimal.ZERO));
            assertNull(head.getString("linkNumber"));

            JSONObject detail = getFirstDetail(headId);
            assertEquals(0, detail.getBigDecimal("unitPrice").compareTo(new BigDecimal("12.345")));
            assertEquals(0, detail.getBigDecimal("allPrice").compareTo(new BigDecimal("24.69")));
            assertEquals(0, detail.getBigDecimal("taxRate").compareTo(BigDecimal.ZERO));
            assertNull(detail.getLong("linkId"));
        } finally {
            deleteDraft(headId);
        }
    }

    @Test
    @DisplayName("其它出库拒绝伪造状态、负金额和非法来源")
    void rejectForgedStateAmountAndSource() {
        assumeTrue(hasBaseData(), "缺少其它出库测试基础数据");
        Response statusResponse = submitOtherOutbound(generateNumber("QTCK"), "3", null, null,
                barCode, unit, depotId, 1, 10, 10);
        assertBizCode(statusResponse, ExceptionConstants.DEPOT_HEAD_OTHER_STATUS_CODE);

        Response amountResponse = submitOtherOutbound(generateNumber("QTCK"), "0", null, null,
                barCode, unit, depotId, 1, -10, -10);
        assertBizCode(amountResponse, ExceptionConstants.DEPOT_HEAD_OTHER_OUT_AMOUNT_CODE);

        Response sourceResponse = submitOtherOutbound(generateNumber("QTCK"), "0", "NOT-A-REAL-SOURCE",
                999999L, barCode, unit, depotId, 1, 0, 0);
        assertBizCode(sourceResponse, ExceptionConstants.DEPOT_HEAD_OTHER_OUT_SOURCE_CODE);
    }

    @Test
    @DisplayName("关联其它出库校验来源明细和累计数量并在审核时复核")
    void validateLinkedOtherOutbound() {
        assumeTrue(hasBaseData() && supplierId != null && accountId != null,
                "缺少创建采购退货来源单所需的基础数据");
        Long sourceId = null;
        Long targetId = null;
        boolean targetAudited = false;
        boolean sourceAudited = false;
        try {
            sourceId = createDepotHeadAndDetail("出库", "采购退货", supplierId, accountId,
                    depotId, materialExtendId, 2, 10);
            assumeTrue(sourceId != null, "无法创建其它出库关联来源单");
            auditDepotHead(sourceId);
            sourceAudited = true;
            JSONObject sourceHead = getRawHead(sourceId);
            JSONObject sourceDetail = getFirstDetail(sourceId);

            Response detailResponse = submitOtherOutbound(generateNumber("QTCK"), "0",
                    sourceHead.getString("number"), 999999999L, sourceDetail.getString("barCode"),
                    "伪造单位", sourceDetail.getLong("depotId"), 1, 999, 999);
            assertBizCode(detailResponse, ExceptionConstants.DEPOT_HEAD_OTHER_OUT_DETAIL_CODE);

            BigDecimal sourceQuantity = sourceDetail.getBigDecimal("operNumber");
            Response overResponse = submitOtherOutbound(generateNumber("QTCK"), "0",
                    sourceHead.getString("number"), sourceDetail.getLong("id"), sourceDetail.getString("barCode"),
                    "伪造单位", sourceDetail.getLong("depotId"),
                    sourceQuantity.add(BigDecimal.ONE).doubleValue(), 999, 999);
            assertBizCode(overResponse, ExceptionConstants.DEPOT_HEAD_OTHER_OUT_OVER_CODE);

            String targetNumber = generateNumber("QTCK");
            assertSuccess(submitOtherOutbound(targetNumber, "0", sourceHead.getString("number"),
                    sourceDetail.getLong("id"), sourceDetail.getString("barCode"), "伪造单位",
                    sourceDetail.getLong("depotId"), sourceQuantity.doubleValue(), 999, 999));
            targetId = getHeadId(targetNumber);
            JSONObject targetHead = getRawHead(targetId);
            JSONObject targetDetail = getFirstDetail(targetId);
            assertEquals(sourceHead.getLong("organId"), targetHead.getLong("organId"));
            assertEquals(sourceDetail.getString("unit"), targetDetail.getString("unit"));
            assertEquals(sourceDetail.getLong("depotId"), targetDetail.getLong("depotId"));
            assertEquals(0, targetDetail.getBigDecimal("unitPrice").compareTo(BigDecimal.ZERO));
            assertEquals(0, targetDetail.getBigDecimal("allPrice").compareTo(BigDecimal.ZERO));
            assertEquals(0, targetHead.getBigDecimal("totalPrice").compareTo(BigDecimal.ZERO));

            auditDepotHead(targetId);
            targetAudited = true;
        } finally {
            if (targetId != null) {
                if (targetAudited) {
                    unauditDepotHead(targetId);
                }
                deleteDraft(targetId);
            }
            if (sourceId != null) {
                if (sourceAudited) {
                    unauditDepotHead(sourceId);
                }
                deleteDraft(sourceId);
            }
        }
    }

    private boolean hasBaseData() {
        return materialExtendId != null && barCode != null && unit != null && depotId != null;
    }

    private Response submitOtherOutbound(String number, String status, String linkNumber, Long linkId,
                                         String submittedBarCode, String submittedUnit, Long submittedDepotId,
                                         double quantity, double unitPrice, double allPrice) {
        JSONObject info = new JSONObject();
        info.put("number", number);
        info.put("type", "出库");
        info.put("subType", "其它");
        info.put("linkNumber", linkNumber);
        info.put("totalPrice", 99999);
        info.put("changeAmount", 99999);
        info.put("discountLastMoney", 99999);
        info.put("otherMoney", 99999);
        info.put("debt", 99999);
        info.put("status", status);

        JSONObject item = new JSONObject();
        item.put("linkId", linkId);
        item.put("barCode", submittedBarCode);
        item.put("unit", submittedUnit);
        item.put("depotId", submittedDepotId);
        item.put("operNumber", quantity);
        item.put("preNumber", 999999);
        item.put("finishNumber", -999999);
        item.put("unitPrice", unitPrice);
        item.put("allPrice", allPrice);
        item.put("taxRate", 99);
        item.put("taxMoney", 99999);
        item.put("taxLastMoney", 99999);

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
        if (headId != null) {
            assertSuccess(authReq().param("id", headId).delete(CONTEXT + "/depotHead/delete"));
        }
    }
}
