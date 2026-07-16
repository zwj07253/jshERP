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

@DisplayName("P0: 零售出库业务校验")
public class P0RetailValidationTest extends ApiTestBase {

    private static String barCode;
    private static String unit;
    private static Long depotId;
    private static Long accountId;

    @BeforeAll
    static void prepareRetailData() {
        P0RetailValidationTest test = new P0RetailValidationTest();
        Response materialResponse = test.authReqGet().param("q", "").param("page", 1).param("rows", 10)
                .get(CONTEXT + "/material/findBySelect");
        JSONObject materialData = JSONObject.parseObject(materialResponse.body().asString());
        if (materialData.getLongValue("total") > 0) {
            JSONObject material = materialData.getJSONArray("rows").getJSONObject(0);
            barCode = material.getString("mBarCode");
            unit = material.getString("unit");
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
    @DisplayName("零售数量为负数时拒绝保存")
    void rejectNegativeRetailQuantity() {
        assumeTrue(hasBaseData(), "缺少商品、仓库或结算账户基础数据");
        Response response = submitRetail(-1, 10, 10);
        assertBizError(response);
        assertEquals(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                JSONObject.parseObject(response.body().asString()).getIntValue("code"));
    }

    @Test
    @DisplayName("零售数量为零时拒绝保存")
    void rejectZeroRetailQuantity() {
        assumeTrue(hasBaseData(), "缺少商品、仓库或结算账户基础数据");
        Response response = submitRetail(0, 10, 10);
        assertBizError(response);
        assertEquals(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                JSONObject.parseObject(response.body().asString()).getIntValue("code"));
    }

    @Test
    @DisplayName("收款金额小于明细金额时拒绝保存")
    void rejectInsufficientRetailReceipt() {
        assumeTrue(hasBaseData(), "缺少商品、仓库或结算账户基础数据");
        Response response = submitRetail(1, 10, 5);
        assertBizError(response);
        assertEquals(ExceptionConstants.DEPOT_HEAD_RETAIL_RECEIPT_LACK_CODE,
                JSONObject.parseObject(response.body().asString()).getIntValue("code"));
    }

    private boolean hasBaseData() {
        return barCode != null && depotId != null && accountId != null;
    }

    private Response submitRetail(double quantity, double unitPrice, double getAmount) {
        JSONObject info = new JSONObject();
        info.put("number", generateNumber("LSCK"));
        info.put("type", "出库");
        info.put("subType", "零售");
        info.put("accountId", accountId);
        info.put("changeAmount", quantity * unitPrice);
        info.put("totalPrice", quantity * unitPrice);
        info.put("getAmount", getAmount);
        info.put("backAmount", getAmount - quantity * unitPrice);
        info.put("payType", "现付");
        info.put("status", "0");

        JSONObject item = new JSONObject();
        item.put("barCode", barCode);
        item.put("depotId", depotId);
        item.put("unit", unit == null ? "" : unit);
        item.put("operNumber", quantity);
        item.put("unitPrice", unitPrice);
        item.put("allPrice", quantity * unitPrice);
        JSONArray rows = new JSONArray();
        rows.add(item);

        JSONObject body = new JSONObject();
        body.put("info", info.toJSONString());
        body.put("rows", rows.toJSONString());
        Response response = authReq().body(body.toJSONString()).post(CONTEXT + "/depotHead/addDepotHeadAndDetail");
        assertNotNull(response);
        return response;
    }
}
