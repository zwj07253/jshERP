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

@DisplayName("P0: 其它入库业务校验")
public class P0OtherInboundValidationTest extends ApiTestBase {

    private static String barCode;
    private static String unit;
    private static Long depotId;

    @BeforeAll
    static void prepareOtherInboundData() {
        P0OtherInboundValidationTest test = new P0OtherInboundValidationTest();
        Response materialResponse = test.authReqGet().param("q", "").param("page", 1).param("rows", 100)
                .get(CONTEXT + "/material/findBySelect");
        JSONArray materials = JSONObject.parseObject(materialResponse.body().asString()).getJSONArray("rows");
        if (materials != null) {
            for (int index = 0; index < materials.size(); index++) {
                JSONObject material = materials.getJSONObject(index);
                if (!"1".equals(material.getString("enableBatchNumber"))
                        && !"1".equals(material.getString("enableSerialNumber"))) {
                    barCode = material.getString("mBarCode");
                    unit = material.getString("unit").replace("[基本]", "");
                    break;
                }
            }
        }
        Response depotResponse = test.authReqGet().param("search", "{}").get(CONTEXT + "/depot/list");
        JSONObject depotData = JSONObject.parseObject(depotResponse.body().asString()).getJSONObject("data");
        if (depotData != null && !depotData.getJSONArray("rows").isEmpty()) {
            depotId = depotData.getJSONArray("rows").getJSONObject(0).getLong("id");
        }
    }

    @Test
    @DisplayName("独立其它入库由服务端重算金额并清空关联字段")
    void normalizeStandaloneOtherInbound() {
        assumeTrue(barCode != null && depotId != null, "缺少其它入库测试基础数据");
        String number = generateNumber("QTRK");
        Long headId = null;
        try {
            Response response = submitOtherInbound(number, "0", null, 999999L,
                    barCode, unit, depotId, 2, 12.345, 99999);
            assertSuccess(response);
            headId = getHeadId(number);
            JSONObject head = getRawHead(headId);
            assertEquals(0, head.getBigDecimal("totalPrice").compareTo(new BigDecimal("-24.69")));
            assertEquals(0, head.getBigDecimal("changeAmount").compareTo(BigDecimal.ZERO));
            assertNull(head.getString("linkNumber"));

            JSONObject detail = getFirstDetail(headId);
            assertEquals(0, detail.getBigDecimal("unitPrice").compareTo(new BigDecimal("12.345")));
            assertEquals(0, detail.getBigDecimal("allPrice").compareTo(new BigDecimal("24.69")));
            assertNull(detail.getLong("linkId"));
        } finally {
            deleteDraft(headId);
        }
    }

    @Test
    @DisplayName("其它入库拒绝伪造状态、非法来源和批量转换非法来源")
    void rejectForgedStateAndSource() {
        assumeTrue(barCode != null && depotId != null, "缺少其它入库测试基础数据");
        Response statusResponse = submitOtherInbound(generateNumber("QTRK"), "3", null, null,
                barCode, unit, depotId, 1, 10, 10);
        assertBizCode(statusResponse, ExceptionConstants.DEPOT_HEAD_OTHER_STATUS_CODE);

        Response amountResponse = submitOtherInbound(generateNumber("QTRK"), "0", null, null,
                barCode, unit, depotId, 1, -10, -10);
        assertBizCode(amountResponse, ExceptionConstants.DEPOT_HEAD_OTHER_IN_AMOUNT_CODE);

        String sourceNumber = generateNumber("QTRK");
        Long sourceId = null;
        try {
            assertSuccess(submitOtherInbound(sourceNumber, "0", null, null,
                    barCode, unit, depotId, 1, 10, 10));
            sourceId = getHeadId(sourceNumber);
            Long sourceItemId = getFirstDetail(sourceId).getLong("id");

            Response linkResponse = submitOtherInbound(generateNumber("QTRK"), "0", sourceNumber,
                    sourceItemId, barCode, unit, depotId, 1, 0, 0);
            assertBizCode(linkResponse, ExceptionConstants.DEPOT_HEAD_OTHER_IN_SOURCE_CODE);

            JSONObject batchBody = new JSONObject();
            batchBody.put("ids", sourceId.toString());
            Response batchResponse = authReq().body(batchBody.toJSONString())
                    .post(CONTEXT + "/depotHead/batchAddDepotHeadAndDetail");
            assertBizCode(batchResponse, ExceptionConstants.DEPOT_HEAD_OTHER_BATCH_SOURCE_CODE);
        } finally {
            deleteDraft(sourceId);
        }
    }

    @Test
    @DisplayName("关联其它入库校验累计数量并将价格归零")
    void validateLinkedOtherInbound() {
        JSONObject source = findAvailableInboundSource();
        assumeTrue(source != null, "缺少可用于其它入库关联测试的采购入库或销售退货单");
        JSONObject sourceDetail = getFirstDetail(source.getLong("id"));
        assumeTrue(!"1".equals(sourceDetail.getString("enableBatchNumber"))
                && !"1".equals(sourceDetail.getString("enableSerialNumber")), "来源商品启用了批号或序列号");

        BigDecimal sourceQuantity = sourceDetail.getBigDecimal("operNumber");
        Response overResponse = submitOtherInbound(generateNumber("QTRK"), "0", source.getString("number"),
                sourceDetail.getLong("id"), sourceDetail.getString("barCode"), "伪造单位",
                sourceDetail.getLong("depotId"), sourceQuantity.add(BigDecimal.ONE).doubleValue(), 999, 999);
        assertBizCode(overResponse, ExceptionConstants.DEPOT_HEAD_OTHER_IN_OVER_CODE);

        String number = generateNumber("QTRK");
        Long headId = null;
        try {
            assertSuccess(submitOtherInbound(number, "0", source.getString("number"),
                    sourceDetail.getLong("id"), sourceDetail.getString("barCode"), "伪造单位",
                    sourceDetail.getLong("depotId"), sourceQuantity.doubleValue(), 999, 999));
            headId = getHeadId(number);
            JSONObject detail = getFirstDetail(headId);
            assertEquals(sourceDetail.getString("unit"), detail.getString("unit"));
            assertEquals(sourceDetail.getLong("depotId"), detail.getLong("depotId"));
            assertEquals(0, detail.getBigDecimal("unitPrice").compareTo(BigDecimal.ZERO));
            assertEquals(0, detail.getBigDecimal("allPrice").compareTo(BigDecimal.ZERO));
            assertEquals(0, getRawHead(headId).getBigDecimal("totalPrice").compareTo(BigDecimal.ZERO));
        } finally {
            deleteDraft(headId);
        }
    }

    private Response submitOtherInbound(String number, String status, String linkNumber, Long linkId,
                                        String submittedBarCode, String submittedUnit, Long submittedDepotId,
                                        double quantity, double unitPrice, double allPrice) {
        JSONObject info = new JSONObject();
        info.put("number", number);
        info.put("type", "入库");
        info.put("subType", "其它");
        info.put("linkNumber", linkNumber);
        info.put("totalPrice", 99999);
        info.put("changeAmount", 99999);
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

    private JSONObject findAvailableInboundSource() {
        for (String subType : new String[]{"采购", "销售退货"}) {
            JSONObject search = new JSONObject();
            search.put("type", "入库");
            search.put("subType", subType);
            search.put("status", "1");
            Response response = authReqGet().param("search", search.toJSONString())
                    .get(CONTEXT + "/depotHead/list");
            JSONObject data = JSONObject.parseObject(response.body().asString()).getJSONObject("data");
            if (data != null && data.getJSONArray("rows") != null && !data.getJSONArray("rows").isEmpty()) {
                return data.getJSONArray("rows").getJSONObject(0);
            }
        }
        return null;
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
