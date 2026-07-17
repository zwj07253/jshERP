package com.jsh.erp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("P0: 销售订单闭环校验")
public class P0SalesOrderValidationTest extends ApiTestBase {

    private static String barCode;
    private static String unit;
    private static Long materialExtendId;
    private static Long customerId;
    private static Long supplierId;
    private static Long depotId;
    private static Long accountId;

    @BeforeAll
    static void prepareData() {
        P0SalesOrderValidationTest test = new P0SalesOrderValidationTest();
        JSONArray materials = JSONObject.parseObject(test.authReqGet().param("q", "").param("page", 1)
                .param("rows", 100).get(CONTEXT + "/material/findBySelect").body().asString()).getJSONArray("rows");
        if (materials != null) {
            for (int index = 0; index < materials.size(); index++) {
                JSONObject material = materials.getJSONObject(index);
                if (!"1".equals(material.getString("enableBatchNumber"))
                        && !"1".equals(material.getString("enableSerialNumber"))) {
                    barCode = material.getString("mBarCode");
                    unit = material.getString("unit").replace("[基本]", "");
                    materialExtendId = material.getLong("id");
                    break;
                }
            }
        }
        customerId = test.getFirstSupplierId("客户");
        supplierId = test.getFirstSupplierId("供应商");
        depotId = test.getFirstId(test.authReqGet().param("search", "{}").get(CONTEXT + "/depot/list"));
        accountId = test.getFirstId(test.authReqGet().param("search", "{}").get(CONTEXT + "/account/list"));
    }

    @BeforeEach
    void refreshAdminSession() {
        loginAsAdmin();
    }

    @Test
    @DisplayName("销售订单校验客户和状态，并由服务端重算金额")
    void validateCustomerStatusAndAmount() {
        assumeTrue(hasBaseData(), "缺少销售订单测试基础数据");
        assertCode(submitSalesOrder(generateNumber("XSDD"), supplierId, null,
                1, 100, 9999, 0, "0"), ExceptionConstants.DEPOT_HEAD_SALES_CUSTOMER_CODE);
        assertCode(submitSalesOrder(generateNumber("XSDD"), customerId, null,
                1, 100, 9999, 0, "2"), ExceptionConstants.DEPOT_HEAD_SALES_STATUS_CODE);
        assertCode(submitSalesOrder(generateNumber("XSDD"), customerId, null,
                1, 100, 9999, 10, "0"), ExceptionConstants.DEPOT_HEAD_ACCOUNT_FAILED_CODE);
        assertCode(submitSalesOutbound(generateNumber("XSCK"), null, null, accountId + 999999,
                1, 100, "0"), ExceptionConstants.DEPOT_HEAD_SALES_ACCOUNT_INVALID_CODE);

        String number = generateNumber("XSDD");
        Long orderId = null;
        try {
            assertSuccess(submitSalesOrder(number, customerId, accountId,
                    2, 100, 9999, 10, "0"));
            orderId = getHeadId(number);
            JSONObject head = getRawHead(orderId);
            JSONObject detail = getFirstDetail(orderId);
            Long originalCreator = head.getLong("creator");
            assertEquals(0, head.getBigDecimal("totalPrice").compareTo(new BigDecimal("200.00")));
            assertEquals(0, head.getBigDecimal("changeAmount").compareTo(new BigDecimal("10.00")));
            assertEquals(0, detail.getBigDecimal("allPrice").compareTo(new BigDecimal("200.00")));

            head.put("creator", 99999999L);
            assertSuccess(updateBill(head, detail));
            assertEquals(originalCreator, getRawHead(orderId).getLong("creator"), "编辑不能覆盖服务端维护字段");

            head.put("number", generateNumber("XSDD"));
            assertCode(updateBill(head, detail), ExceptionConstants.DEPOT_HEAD_SALES_LINK_CHANGE_CODE);
        } finally {
            deleteIfPresent(orderId);
        }
    }

    @Test
    @DisplayName("销售出库校验来源与累计数量，且只在审核后回写订单进度")
    void validateOutboundLinkAndProgress() {
        assumeTrue(hasBaseData(), "缺少销售出库测试基础数据");
        String orderNumber = generateNumber("XSDD");
        Long orderId = null;
        Long outboundId = null;
        Long secondOutboundId = null;
        try {
            assertSuccess(submitSalesOrder(orderNumber, customerId, null,
                    2, 100, 1, 0, "1"));
            orderId = getHeadId(orderNumber);
            Long sourceItemId = getFirstDetail(orderId).getLong("id");

            assertCode(submitSalesOutbound(generateNumber("XSCK"), orderNumber, sourceItemId,
                    3, 999, "0"), ExceptionConstants.DEPOT_HEAD_SALES_OUT_OVER_CODE);
            assertCode(submitSalesOutbound(generateNumber("XSCK"), orderNumber, sourceItemId + 999999,
                    1, 999, "0"), ExceptionConstants.DEPOT_HEAD_SALES_OUT_DETAIL_CODE);

            String outboundNumber = generateNumber("XSCK");
            assertSuccess(submitSalesOutbound(outboundNumber, orderNumber, sourceItemId,
                    1, 999, "0"));
            outboundId = getHeadId(outboundNumber);
            assertEquals(0, getFirstDetail(outboundId).getBigDecimal("unitPrice")
                    .compareTo(new BigDecimal("100.000000")));
            assertEquals("1", getRawHead(orderId).getString("status"), "未审核出库不能改变订单进度");
            assertCode(setDepotHeadStatus(orderId, "0"),
                    ExceptionConstants.DEPOT_HEAD_SALES_ORDER_HAS_OUTBOUND_CODE);

            auditDepotHead(outboundId);
            assertEquals("3", getRawHead(orderId).getString("status"));
            unauditDepotHead(outboundId);
            assertEquals("1", getRawHead(orderId).getString("status"));

            auditDepotHead(outboundId);
            String secondOutboundNumber = generateNumber("XSCK");
            assertSuccess(submitSalesOutbound(secondOutboundNumber, orderNumber, sourceItemId,
                    1, 999, "1"));
            secondOutboundId = getHeadId(secondOutboundNumber);
            assertEquals("2", getRawHead(orderId).getString("status"));
            unauditDepotHead(secondOutboundId);
            assertEquals("3", getRawHead(orderId).getString("status"));
            deleteIfPresent(secondOutboundId);
            secondOutboundId = null;
            assertEquals("3", getRawHead(orderId).getString("status"));
            unauditDepotHead(outboundId);
            assertEquals("1", getRawHead(orderId).getString("status"));
        } finally {
            if (secondOutboundId != null && "1".equals(getRawHead(secondOutboundId).getString("status"))) {
                unauditDepotHead(secondOutboundId);
            }
            deleteIfPresent(secondOutboundId);
            if (outboundId != null && "1".equals(getRawHead(outboundId).getString("status"))) {
                unauditDepotHead(outboundId);
            }
            deleteIfPresent(outboundId);
            if (orderId != null) {
                JSONObject order = getRawHead(orderId);
                if ("1".equals(order.getString("status"))) {
                    unauditDepotHead(orderId);
                }
                deleteIfPresent(orderId);
            }
        }
    }

    private boolean hasBaseData() {
        return barCode != null && unit != null && customerId != null && supplierId != null
                && depotId != null && accountId != null;
    }

    private Long getFirstSupplierId(String type) {
        Response response = authReqGet().param("search", "{\"type\":\"" + type + "\"}")
                .get(CONTEXT + "/supplier/list");
        JSONObject data = JSONObject.parseObject(response.body().asString()).getJSONObject("data");
        JSONArray rows = data == null ? null : data.getJSONArray("rows");
        return rows == null || rows.isEmpty() ? null : rows.getJSONObject(0).getLong("id");
    }

    private Response submitSalesOrder(String number, Long organId, Long settlementAccountId,
                                      double quantity, double unitPrice, double submittedAmount,
                                      double deposit, String status) {
        JSONObject info = new JSONObject();
        info.put("number", number);
        info.put("type", "其它");
        info.put("subType", "销售订单");
        info.put("organId", organId);
        info.put("accountId", settlementAccountId);
        info.put("changeAmount", deposit);
        info.put("totalPrice", submittedAmount);
        info.put("status", status);
        return submitBill(info, buildItem(null, quantity, unitPrice, submittedAmount, null));
    }

    private Response submitSalesOutbound(String number, String linkNumber, Long linkId,
                                         double quantity, double submittedPrice, String status) {
        return submitSalesOutbound(number, linkNumber, linkId, accountId, quantity, submittedPrice, status);
    }

    private Response submitSalesOutbound(String number, String linkNumber, Long linkId, Long settlementAccountId,
                                         double quantity, double submittedPrice, String status) {
        JSONObject info = new JSONObject();
        info.put("number", number);
        info.put("type", "出库");
        info.put("subType", "销售");
        info.put("organId", customerId);
        info.put("accountId", settlementAccountId);
        info.put("linkNumber", linkNumber);
        info.put("changeAmount", quantity * 100);
        info.put("deposit", 0);
        info.put("totalPrice", quantity * submittedPrice);
        info.put("status", status);
        return submitBill(info, buildItem(linkId, quantity, submittedPrice,
                quantity * submittedPrice, depotId));
    }

    private Response setDepotHeadStatus(Long id, String status) {
        JSONObject body = new JSONObject();
        body.put("status", status);
        body.put("ids", String.valueOf(id));
        return authReq().body(body.toJSONString()).post(CONTEXT + "/depotHead/batchSetStatus");
    }

    private JSONObject buildItem(Long linkId, double quantity, double unitPrice,
                                 double submittedAmount, Long detailDepotId) {
        JSONObject item = new JSONObject();
        item.put("linkId", linkId);
        item.put("barCode", barCode);
        item.put("unit", unit);
        item.put("depotId", detailDepotId);
        item.put("operNumber", quantity);
        item.put("unitPrice", unitPrice);
        item.put("allPrice", submittedAmount);
        item.put("taxRate", 0);
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
        Response response = authReqGet().param("number", number).get(CONTEXT + "/depotHead/getDetailByNumber");
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
