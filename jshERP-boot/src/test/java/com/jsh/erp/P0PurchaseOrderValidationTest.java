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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("P0: 采购订单闭环校验")
public class P0PurchaseOrderValidationTest extends ApiTestBase {

    private static String barCode;
    private static String unit;
    private static Long depotId;
    private static Long accountId;

    @BeforeAll
    static void prepareData() {
        P0PurchaseOrderValidationTest test = new P0PurchaseOrderValidationTest();
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
        depotId = test.getFirstId(depotResponse);
        Response accountResponse = test.authReqGet().param("search", "{}").get(CONTEXT + "/account/list");
        accountId = test.getFirstId(accountResponse);
    }

    @Test
    @DisplayName("采购订单校验供应商并由服务端重算金额")
    void validateSupplierAndNormalizeAmount() {
        assumeTrue(hasBaseData(), "缺少采购订单测试基础数据");
        Response noSupplier = submitPurchaseOrder(generateNumber("CGDD"), null, null, null,
                1, unit, 10, 9999, 13, "0");
        assertCode(noSupplier, ExceptionConstants.DEPOT_HEAD_PURCHASE_SUPPLIER_CODE);

        String number = generateNumber("CGDD");
        Long headId = null;
        try {
            assertSuccess(submitPurchaseOrder(number, 101L, null, null,
                    2, unit, 10, 9999, 13, "0"));
            headId = getHeadId(number);
            JSONObject head = getRawHead(headId);
            JSONObject detail = getFirstDetail(headId);
            assertEquals(0, head.getBigDecimal("totalPrice").compareTo(new BigDecimal("-20.00")));
            assertEquals(0, detail.getBigDecimal("allPrice").compareTo(new BigDecimal("20.00")));
            assertEquals(0, detail.getBigDecimal("taxMoney").compareTo(new BigDecimal("2.60")));
            assertEquals(0, detail.getBigDecimal("taxLastMoney").compareTo(new BigDecimal("22.60")));
        } finally {
            deleteIfPresent(headId);
        }
    }

    @Test
    @DisplayName("以销定购校验来源明细、累计数量并回写采购状态")
    void validateSalesOrderLink() {
        assumeTrue(hasBaseData(), "缺少采购订单测试基础数据");
        String salesNumber = generateNumber("XSDD");
        String purchaseNumber = generateNumber("CGDD");
        Long salesId = null;
        Long purchaseId = null;
        try {
            assertSuccess(submitSalesOrder(salesNumber, 2));
            salesId = getHeadId(salesNumber);
            Long sourceItemId = getFirstDetail(salesId).getLong("id");

            Response over = submitPurchaseOrder(generateNumber("CGDD"), 101L, salesNumber, sourceItemId,
                    3, unit, 10, 30, 0, "0");
            assertCode(over, ExceptionConstants.DEPOT_HEAD_PURCHASE_SALES_OVER_CODE);

            assertSuccess(submitPurchaseOrder(purchaseNumber, 101L, salesNumber, sourceItemId,
                    1, "伪造单位", 10, 999, 0, "0"));
            purchaseId = getHeadId(purchaseNumber);
            assertEquals(unit, getFirstDetail(purchaseId).getString("unit"));
            assertEquals("3", getRawHead(salesId).getString("purchaseStatus"));

            assertSuccess(authReq().param("id", purchaseId).delete(CONTEXT + "/depotHead/delete"));
            purchaseId = null;
            assertEquals("0", getRawHead(salesId).getString("purchaseStatus"));
        } finally {
            deleteIfPresent(purchaseId);
            if (salesId != null) {
                JSONObject sales = getRawHead(salesId);
                if ("1".equals(sales.getString("status"))) {
                    unauditDepotHead(salesId);
                }
                deleteIfPresent(salesId);
            }
        }
    }

    @Test
    @DisplayName("采购订单转采购入库校验供应商、来源价格和累计入库数量")
    void validatePurchaseInboundLink() {
        assumeTrue(hasBaseData(), "缺少采购订单测试基础数据");
        String orderNumber = generateNumber("CGDD");
        Long orderId = null;
        Long firstInboundId = null;
        Long secondInboundId = null;
        try {
            assertSuccess(submitPurchaseOrder(orderNumber, 101L, null, null,
                    2, unit, 10, 999, 0, "1"));
            orderId = getHeadId(orderNumber);
            Long sourceItemId = getFirstDetail(orderId).getLong("id");

            Response wrongSupplier = submitPurchaseInbound(generateNumber("CGRK"), 102L, orderNumber,
                    sourceItemId, 1, 999);
            assertCode(wrongSupplier, ExceptionConstants.DEPOT_HEAD_PURCHASE_SUPPLIER_CODE);

            Response over = submitPurchaseInbound(generateNumber("CGRK"), 101L, orderNumber,
                    sourceItemId, 3, 999);
            assertCode(over, ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_OVER_CODE);

            String firstNumber = generateNumber("CGRK");
            assertSuccess(submitPurchaseInbound(firstNumber, 101L, orderNumber, sourceItemId, 1, 999));
            firstInboundId = getHeadId(firstNumber);
            assertEquals(0, getFirstDetail(firstInboundId).getBigDecimal("unitPrice")
                    .compareTo(new BigDecimal("10.000000")));
            JSONObject renamedHead = getRawHead(firstInboundId);
            renamedHead.put("number", generateNumber("CGRK"));
            Response renameResponse = updateBill(renamedHead, getFirstDetail(firstInboundId));
            assertCode(renameResponse, ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_NUMBER_CHANGE_CODE);
            assertEquals("3", getRawHead(orderId).getString("status"));

            String secondNumber = generateNumber("CGRK");
            assertSuccess(submitPurchaseInbound(secondNumber, 101L, orderNumber, sourceItemId, 1, 999));
            secondInboundId = getHeadId(secondNumber);
            assertEquals("2", getRawHead(orderId).getString("status"));

            assertSuccess(authReq().param("id", secondInboundId).delete(CONTEXT + "/depotHead/delete"));
            secondInboundId = null;
            assertEquals("3", getRawHead(orderId).getString("status"));
            assertSuccess(authReq().param("id", firstInboundId).delete(CONTEXT + "/depotHead/delete"));
            firstInboundId = null;
            assertEquals("1", getRawHead(orderId).getString("status"));
        } finally {
            deleteIfPresent(secondInboundId);
            deleteIfPresent(firstInboundId);
            if (orderId != null) {
                JSONObject order = getRawHead(orderId);
                if ("1".equals(order.getString("status"))) {
                    unauditDepotHead(orderId);
                }
                deleteIfPresent(orderId);
            }
        }
    }

    @Test
    @DisplayName("采购入库拒绝伪造完成状态和无来源订金")
    void rejectForgedStatusAndUnlinkedDeposit() {
        assumeTrue(hasBaseData(), "缺少采购入库测试基础数据");

        Response forgedStatus = submitPurchaseInbound(generateNumber("CGRK"), 101L, null, null,
                1, 10, 0, "2", unit);
        assertCode(forgedStatus, ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_STATUS_CODE);

        Response unlinkedDeposit = submitPurchaseInbound(generateNumber("CGRK"), 101L, null, null,
                1, 10, 1, "0", unit);
        assertCode(unlinkedDeposit, ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_DEPOSIT_SOURCE_CODE);
    }

    @Test
    @DisplayName("存在采购退货时禁止反审核原采购入库")
    void blockUnauditWhenPurchaseReturnExists() {
        assumeTrue(hasBaseData(), "缺少采购入库测试基础数据");
        String orderNumber = generateNumber("CGDD");
        String inboundNumber = generateNumber("CGRK");
        Long orderId = null;
        Long inboundId = null;
        Long returnId = null;
        try {
            assertSuccess(submitPurchaseOrder(orderNumber, 101L, null, null,
                    1, unit, 10, 10, 0, "1"));
            orderId = getHeadId(orderNumber);
            Long orderItemId = getFirstDetail(orderId).getLong("id");
            assertSuccess(submitPurchaseInbound(inboundNumber, 101L, orderNumber, orderItemId, 1, 10));
            inboundId = getHeadId(inboundNumber);
            auditDepotHead(inboundId);

            Long inboundItemId = getFirstDetail(inboundId).getLong("id");
            String returnNumber = generateNumber("CGTH");
            assertSuccess(submitPurchaseReturn(returnNumber, inboundNumber, inboundItemId));
            returnId = getHeadId(returnNumber);

            JSONObject statusBody = new JSONObject();
            statusBody.put("status", "0");
            statusBody.put("ids", String.valueOf(inboundId));
            Response unauditResponse = authReq().body(statusBody.toJSONString())
                    .post(CONTEXT + "/depotHead/batchSetStatus");
            assertCode(unauditResponse, ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_HAS_RETURN_CODE);
        } finally {
            deleteIfPresent(returnId);
            if (inboundId != null && "1".equals(getRawHead(inboundId).getString("status"))) {
                unauditDepotHead(inboundId);
            }
            deleteIfPresent(inboundId);
            if (orderId != null && "1".equals(getRawHead(orderId).getString("status"))) {
                unauditDepotHead(orderId);
            }
            deleteIfPresent(orderId);
        }
    }

    private boolean hasBaseData() {
        return barCode != null && unit != null && depotId != null && accountId != null;
    }

    private Response submitSalesOrder(String number, double quantity) {
        JSONObject info = new JSONObject();
        info.put("number", number);
        info.put("type", "其它");
        info.put("subType", "销售订单");
        info.put("organId", 105);
        info.put("changeAmount", 0);
        info.put("totalPrice", quantity * 20);
        info.put("status", "1");
        JSONObject item = buildItem(null, quantity, unit, 20, quantity * 20, 0);
        return submitBill(info, item);
    }

    private Response submitPurchaseOrder(String number, Long supplierId, String linkNumber, Long linkId,
                                         double quantity, String submittedUnit, double unitPrice,
                                         double submittedAmount, double taxRate, String status) {
        JSONObject info = new JSONObject();
        info.put("number", number);
        info.put("type", "其它");
        info.put("subType", "采购订单");
        info.put("organId", supplierId);
        info.put("linkNumber", linkNumber);
        info.put("changeAmount", 0);
        info.put("totalPrice", submittedAmount);
        info.put("status", status);
        JSONObject item = buildItem(linkId, quantity, submittedUnit, unitPrice, submittedAmount, taxRate);
        return submitBill(info, item);
    }

    private Response submitPurchaseInbound(String number, Long supplierId, String linkNumber, Long linkId,
                                           double quantity, double submittedUnitPrice) {
        return submitPurchaseInbound(number, supplierId, linkNumber, linkId, quantity,
                submittedUnitPrice, 0, "0", "伪造单位");
    }

    private Response submitPurchaseInbound(String number, Long supplierId, String linkNumber, Long linkId,
                                           double quantity, double submittedUnitPrice, double deposit,
                                           String status, String submittedUnit) {
        JSONObject info = new JSONObject();
        info.put("number", number);
        info.put("type", "入库");
        info.put("subType", "采购");
        info.put("organId", supplierId);
        info.put("accountId", accountId);
        info.put("linkNumber", linkNumber);
        info.put("changeAmount", 0);
        info.put("deposit", deposit);
        info.put("totalPrice", submittedUnitPrice * quantity);
        info.put("status", status);
        JSONObject item = buildItem(linkId, quantity, submittedUnit, submittedUnitPrice,
                submittedUnitPrice * quantity, 0);
        item.put("depotId", depotId);
        return submitBill(info, item);
    }

    private Response submitPurchaseReturn(String number, String linkNumber, Long linkId) {
        JSONObject info = new JSONObject();
        info.put("number", number);
        info.put("type", "出库");
        info.put("subType", "采购退货");
        info.put("organId", 101L);
        info.put("accountId", accountId);
        info.put("linkNumber", linkNumber);
        info.put("changeAmount", 10);
        info.put("totalPrice", 10);
        info.put("deposit", 0);
        info.put("status", "0");
        JSONObject item = buildItem(linkId, 1, unit, 10, 10, 0);
        item.put("depotId", depotId);
        return submitBill(info, item);
    }

    private JSONObject buildItem(Long linkId, double quantity, String submittedUnit, double unitPrice,
                                 double submittedAmount, double taxRate) {
        JSONObject item = new JSONObject();
        item.put("linkId", linkId);
        item.put("barCode", barCode);
        item.put("unit", submittedUnit);
        item.put("operNumber", quantity);
        item.put("preNumber", 999);
        item.put("finishNumber", 0);
        item.put("unitPrice", unitPrice);
        item.put("allPrice", submittedAmount);
        item.put("taxRate", taxRate);
        item.put("taxMoney", submittedAmount);
        item.put("taxLastMoney", submittedAmount);
        return item;
    }

    private Response submitBill(JSONObject info, JSONObject item) {
        JSONArray rows = new JSONArray();
        rows.add(item);
        JSONObject body = new JSONObject();
        body.put("info", info.toJSONString());
        body.put("rows", rows.toJSONString());
        return authReq().body(body.toJSONString()).post(CONTEXT + "/depotHead/addDepotHeadAndDetail");
    }

    private Response updateBill(JSONObject info, JSONObject item) {
        JSONArray rows = new JSONArray();
        rows.add(item);
        JSONObject body = new JSONObject();
        body.put("info", info.toJSONString());
        body.put("rows", rows.toJSONString());
        return authReq().body(body.toJSONString()).put(CONTEXT + "/depotHead/updateDepotHeadAndDetail");
    }

    private void assertCode(Response response, int code) {
        assertBizError(response);
        assertEquals(code, JSONObject.parseObject(response.body().asString()).getIntValue("code"));
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

    private void deleteIfPresent(Long id) {
        if (id != null) {
            authReq().param("id", id).delete(CONTEXT + "/depotHead/delete");
        }
    }
}
