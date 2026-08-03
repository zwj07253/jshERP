package com.jsh.erp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("P0: 零售退货业务校验")
public class P0RetailReturnValidationTest extends ApiTestBase {

    private static String barCode;
    private static String unit;
    private static Long depotId;
    private static Long accountId;

    @BeforeAll
    static void prepareRetailReturnData() {
        P0RetailReturnValidationTest test = new P0RetailReturnValidationTest();
        Response materialResponse = test.authReqGet().param("q", "").param("page", 1).param("rows", 100)
                .get(CONTEXT + "/material/findBySelect");
        JSONArray materials = JSONObject.parseObject(materialResponse.body().asString()).getJSONArray("rows");
        if (materials != null) {
            for (int index = 0; index < materials.size(); index++) {
                JSONObject material = materials.getJSONObject(index);
                if (!"1".equals(material.getString("enableBatchNumber"))
                        && !"1".equals(material.getString("enableSerialNumber"))) {
                    barCode = material.getString("mBarCode");
                    unit = material.getString("unit").replaceAll("\\[[^]]*]$", "");
                    break;
                }
            }
        }

        Response depotResponse = test.authReqGet().param("search", "{}").get(CONTEXT + "/depot/list");
        JSONObject depotData = JSONObject.parseObject(depotResponse.body().asString()).getJSONObject("data");
        if (depotData.getLongValue("total") > 0) {
            depotId = depotData.getJSONArray("rows").getJSONObject(0).getLong("id");
        }

        Response accountResponse = test.authReqGet().param("search", "{}").get(CONTEXT + "/account/list");
        JSONObject accountData = JSONObject.parseObject(accountResponse.body().asString()).getJSONObject("data");
        if (accountData.getLongValue("total") > 0) {
            accountId = accountData.getJSONArray("rows").getJSONObject(0).getLong("id");
        }
    }

    @Test
    @DisplayName("关联不存在的零售出库单时拒绝保存")
    void rejectMissingRetailSource() {
        assumeTrue(hasBaseData(), "缺少普通商品、仓库或结算账户基础数据");
        Response response = submitRetailReturn(generateNumber("LSTH"), "NOT-EXISTS", 10, 10, 10);
        assertBizError(response);
        assertEquals(ExceptionConstants.DEPOT_HEAD_RETAIL_RETURN_SOURCE_CODE,
                JSONObject.parseObject(response.body().asString()).getIntValue("code"));
    }

    @Test
    @DisplayName("现金退款金额小于退货金额时拒绝保存")
    void rejectInsufficientRetailRefund() {
        assumeTrue(hasBaseData(), "缺少普通商品、仓库或结算账户基础数据");
        Response response = submitRetailReturn(generateNumber("LSTH"), null, 10, 99999, 5);
        assertBizError(response);
        assertEquals(ExceptionConstants.DEPOT_HEAD_RETAIL_RETURN_REFUND_CODE,
                JSONObject.parseObject(response.body().asString()).getIntValue("code"));
    }

    @Test
    @DisplayName("服务端按退货明细重算负数主单金额")
    void normalizeRetailReturnAmount() {
        assumeTrue(hasBaseData(), "缺少普通商品、仓库或结算账户基础数据");
        String number = generateNumber("LSTH");
        Long headId = null;
        try {
            Response response = submitRetailReturn(number, null, 10, 99999, 10);
            assertSuccess(response);

            Response detailResponse = authReqGet().param("number", number)
                    .get(CONTEXT + "/depotHead/getDetailByNumber");
            assertSuccess(detailResponse);
            JSONObject head = JSONObject.parseObject(detailResponse.body().asString()).getJSONObject("data");
            headId = head.getLong("id");
            assertNotNull(headId);
            Response infoResponse = authReqGet().param("id", headId).get(CONTEXT + "/depotHead/info");
            assertSuccess(infoResponse);
            JSONObject rawHead = JSONObject.parseObject(infoResponse.body().asString())
                    .getJSONObject("data").getJSONObject("info");
            assertEquals(0, rawHead.getBigDecimal("totalPrice").compareTo(new java.math.BigDecimal("-10.00")));
            assertEquals(0, rawHead.getBigDecimal("changeAmount").compareTo(new java.math.BigDecimal("-10.00")));
            assertEquals("现付", rawHead.getString("payType"));
        } finally {
            if (headId != null) {
                assertSuccess(authReq().param("id", headId).delete(CONTEXT + "/depotHead/delete"));
            }
        }
    }

    private boolean hasBaseData() {
        return barCode != null && depotId != null && accountId != null;
    }

    private Response submitRetailReturn(String number, String linkNumber, double unitPrice,
                                        double submittedAmount, double getAmount) {
        JSONObject info = new JSONObject();
        info.put("number", number);
        info.put("type", "入库");
        info.put("subType", "零售退货");
        info.put("linkNumber", linkNumber);
        info.put("accountId", accountId);
        info.put("changeAmount", submittedAmount);
        info.put("totalPrice", submittedAmount);
        info.put("getAmount", getAmount);
        info.put("backAmount", 0);
        info.put("payType", "现付");
        info.put("status", "0");

        JSONObject item = new JSONObject();
        item.put("barCode", barCode);
        item.put("depotId", depotId);
        item.put("unit", unit == null ? "" : unit);
        item.put("operNumber", 1);
        item.put("unitPrice", unitPrice);
        item.put("allPrice", submittedAmount);
        JSONArray rows = new JSONArray();
        rows.add(item);

        JSONObject body = new JSONObject();
        body.put("info", info.toJSONString());
        body.put("rows", rows.toJSONString());
        return authReq().body(body.toJSONString()).post(CONTEXT + "/depotHead/addDepotHeadAndDetail");
    }
}
