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

@DisplayName("P0: 请购单业务校验")
public class P0PurchaseApplyValidationTest extends ApiTestBase {

    private static String barCode;
    private static String unit;

    @BeforeAll
    static void preparePurchaseApplyData() {
        P0PurchaseApplyValidationTest test = new P0PurchaseApplyValidationTest();
        Response response = test.authReqGet().param("q", "").param("page", 1).param("rows", 100)
                .get(CONTEXT + "/material/findBySelect");
        JSONArray materials = JSONObject.parseObject(response.body().asString()).getJSONArray("rows");
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
    }

    @Test
    @DisplayName("请购单只保存数量并由服务端清空金额")
    void normalizePurchaseApplyFinancialFields() {
        assumeTrue(barCode != null, "缺少可用于请购单测试的普通商品");
        String number = generateNumber("QGD");
        Long headId = null;
        try {
            Response response = submitPurchaseApply(number, "伪造单位", 1, "0", "请购金额归零测试");
            assertSuccess(response);
            headId = getHeadId(number);

            JSONObject info = getRawHead(headId);
            assertEquals(0, info.getBigDecimal("totalPrice").compareTo(BigDecimal.ZERO));
            assertEquals(0, info.getBigDecimal("changeAmount").compareTo(BigDecimal.ZERO));
            assertNull(info.getLong("organId"));
            assertNull(info.getLong("accountId"));

            JSONObject detail = getFirstDetail(headId);
            assertEquals(unit, detail.getString("unit"));
            assertEquals(0, detail.getBigDecimal("unitPrice").compareTo(BigDecimal.ZERO));
            assertEquals(0, detail.getBigDecimal("allPrice").compareTo(BigDecimal.ZERO));
        } finally {
            if (headId != null) {
                assertSuccess(authReq().param("id", headId).delete(CONTEXT + "/depotHead/delete"));
            }
        }
    }

    @Test
    @DisplayName("采购订单不能关联不存在的请购单")
    void rejectMissingPurchaseApply() {
        assumeTrue(barCode != null, "缺少可用于请购单测试的普通商品");
        Response response = submitPurchaseOrder(generateNumber("CGDD"), "NOT-EXISTS", 999999L,
                1, "伪造单位", "0");
        assertBizError(response);
        assertEquals(ExceptionConstants.DEPOT_HEAD_PURCHASE_APPLY_SOURCE_CODE,
                JSONObject.parseObject(response.body().asString()).getIntValue("code"));
    }

    @Test
    @DisplayName("关联采购订单校验累计数量并保留强制结单前备注")
    void validateLinkedPurchaseOrderAndForceCloseRemark() {
        assumeTrue(barCode != null, "缺少可用于请购单测试的普通商品");
        String applyNumber = generateNumber("QGD");
        String orderNumber = generateNumber("CGDD");
        Long applyId = null;
        Long orderId = null;
        try {
            assertSuccess(submitPurchaseApply(applyNumber, unit, 2, "1", "原请购备注"));
            applyId = getHeadId(applyNumber);
            Long sourceItemId = getFirstDetail(applyId).getLong("id");

            Response overResponse = submitPurchaseOrder(generateNumber("CGDD"), applyNumber, sourceItemId,
                    3, "伪造单位", "0");
            assertBizError(overResponse);
            assertEquals(ExceptionConstants.DEPOT_HEAD_PURCHASE_APPLY_OVER_CODE,
                    JSONObject.parseObject(overResponse.body().asString()).getIntValue("code"));

            assertSuccess(submitPurchaseOrder(orderNumber, applyNumber, sourceItemId,
                    1, "伪造单位", "0"));
            orderId = getHeadId(orderNumber);
            assertEquals(unit, getFirstDetail(orderId).getString("unit"));
            assertEquals("3", getRawHead(applyId).getString("status"));

            JSONObject forceCloseBody = new JSONObject();
            forceCloseBody.put("ids", applyId.toString());
            assertSuccess(authReq().body(forceCloseBody.toJSONString())
                    .post(CONTEXT + "/depotHead/forceCloseBatch"));
            JSONObject closedApply = getRawHead(applyId);
            assertEquals("2", closedApply.getString("status"));
            assertEquals("原请购备注[强制结单]", closedApply.getString("remark"));

            assertSuccess(authReq().param("id", orderId).delete(CONTEXT + "/depotHead/delete"));
            orderId = null;
            assertEquals("1", getRawHead(applyId).getString("status"));
            unauditDepotHead(applyId);
            assertSuccess(authReq().param("id", applyId).delete(CONTEXT + "/depotHead/delete"));
            applyId = null;
        } finally {
            if (orderId != null) {
                authReq().param("id", orderId).delete(CONTEXT + "/depotHead/delete");
            }
            if (applyId != null && "1".equals(getRawHead(applyId).getString("status"))) {
                unauditDepotHead(applyId);
                authReq().param("id", applyId).delete(CONTEXT + "/depotHead/delete");
            }
        }
    }

    private Response submitPurchaseApply(String number, String submittedUnit, double quantity,
                                         String status, String remark) {
        JSONObject info = new JSONObject();
        info.put("number", number);
        info.put("type", "其它");
        info.put("subType", "请购单");
        info.put("organId", 101);
        info.put("accountId", 101);
        info.put("changeAmount", 99999);
        info.put("totalPrice", 99999);
        info.put("status", status);
        info.put("remark", remark);

        JSONObject item = new JSONObject();
        item.put("barCode", barCode);
        item.put("unit", submittedUnit);
        item.put("operNumber", quantity);
        item.put("unitPrice", 999);
        item.put("taxUnitPrice", 999);
        item.put("allPrice", 999);
        item.put("taxRate", 13);
        item.put("taxMoney", 99);
        item.put("taxLastMoney", 1098);
        return submitBill(info, item);
    }

    private Response submitPurchaseOrder(String number, String linkApply, Long linkId, double quantity,
                                         String submittedUnit, String status) {
        JSONObject info = new JSONObject();
        info.put("number", number);
        info.put("type", "其它");
        info.put("subType", "采购订单");
        info.put("linkApply", linkApply);
        info.put("changeAmount", 0);
        info.put("totalPrice", 10 * quantity);
        info.put("status", status);

        JSONObject item = new JSONObject();
        item.put("linkId", linkId);
        item.put("barCode", barCode);
        item.put("unit", submittedUnit);
        item.put("operNumber", quantity);
        item.put("preNumber", 999);
        item.put("finishNumber", 0);
        item.put("unitPrice", 10);
        item.put("allPrice", 10 * quantity);
        item.put("taxRate", 0);
        item.put("taxMoney", 0);
        item.put("taxLastMoney", 10 * quantity);
        return submitBill(info, item);
    }

    private Response submitBill(JSONObject info, JSONObject item) {
        JSONArray rows = new JSONArray();
        rows.add(item);
        JSONObject body = new JSONObject();
        body.put("info", info.toJSONString());
        body.put("rows", rows.toJSONString());
        return authReq().body(body.toJSONString()).post(CONTEXT + "/depotHead/addDepotHeadAndDetail");
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
        return JSONObject.parseObject(response.body().asString())
                .getJSONObject("data").getJSONObject("info");
    }

    private JSONObject getFirstDetail(Long headId) {
        Response response = authReqGet().param("headerId", headId).param("mpList", "")
                .param("linkType", "basic").get(CONTEXT + "/depotItem/getDetailList");
        assertSuccess(response);
        return JSONObject.parseObject(response.body().asString()).getJSONObject("data")
                .getJSONArray("rows").getJSONObject(0);
    }
}
