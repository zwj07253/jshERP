package com.jsh.erp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("P0: 收入单权限与账务校验")
public class P0IncomeValidationTest extends ApiTestBase {

    private static String jshToken;
    private static Long accountId;
    private static Long incomeItemId;
    private static Long expenseItemId;

    @BeforeAll
    static void prepareIncomeData() {
        jshToken = login("jsh", md5("123456"));
        if (jshToken == null) {
            return;
        }
        Response accountResponse = tokenGet().get(CONTEXT + "/account/getAccount");
        JSONObject accountBody = JSONObject.parseObject(accountResponse.body().asString());
        if (accountBody.getIntValue("code") == 200) {
            JSONArray accounts = accountBody.getJSONObject("data").getJSONArray("accountList");
            if (accounts != null && !accounts.isEmpty()) {
                accountId = accounts.getJSONObject(0).getLong("id");
            }
        }
        JSONArray incomeItems = getItems("in");
        if (incomeItems != null && !incomeItems.isEmpty()) {
            incomeItemId = incomeItems.getJSONObject(0).getLong("id");
        }
        JSONArray expenseItems = getItems("out");
        if (expenseItems != null && !expenseItems.isEmpty()) {
            expenseItemId = expenseItems.getJSONObject(0).getLong("id");
        }
    }

    @Test
    @DisplayName("收入单只记主表账户并拒绝修改已审核单据")
    void saveAuditAndProtectAuditedBill() {
        assumeTrue(hasBaseData(), "缺少收入单测试基础数据");
        String billNo = generateNumber("SR");
        Long headId = null;
        boolean audited = false;
        try {
            Response addResponse = submit(billNo, "0", 100, 100, incomeItemId, null, null);
            assertSuccess(addResponse);
            headId = getHeadId(billNo);
            assertNotNull(headId);

            JSONArray details = getDetails(headId);
            assertEquals(1, details.size());
            assertNull(details.getJSONObject(0).get("accountId"), "收入账户只能保存在主表，不能重复写入明细");

            assertSuccess(setStatus(headId, "1"));
            audited = true;
            Response updateResponse = update(headId, billNo, "0", 100, 100, incomeItemId, null);
            assertBizCode(updateResponse, ExceptionConstants.ACCOUNT_HEAD_STATUS_FAILED_CODE);
        } finally {
            if (headId != null) {
                if (audited) {
                    assertSuccess(setStatus(headId, "0"));
                }
                assertSuccess(tokenReq().param("id", headId).delete(CONTEXT + "/accountHead/delete"));
            }
        }
    }

    @Test
    @DisplayName("拒绝金额不一致、负数、支出项目、明细账户和非法状态")
    void rejectForgedIncomeData() {
        assumeTrue(hasBaseData() && expenseItemId != null, "缺少收入单测试基础数据");
        assertBizCode(submit(generateNumber("SR"), "0", 100, 99, incomeItemId, null, null),
                ExceptionConstants.ACCOUNT_HEAD_INCOME_AMOUNT_FAILED_CODE);
        assertBizCode(submit(generateNumber("SR"), "0", -1, -1, incomeItemId, null, null),
                ExceptionConstants.ACCOUNT_HEAD_INCOME_DETAIL_FAILED_CODE);
        assertBizCode(submit(generateNumber("SR"), "0", 100, 100, expenseItemId, null, null),
                ExceptionConstants.ACCOUNT_HEAD_INCOME_DETAIL_FAILED_CODE);
        assertBizCode(submit(generateNumber("SR"), "0", 100, 100, incomeItemId, accountId, null),
                ExceptionConstants.ACCOUNT_HEAD_INCOME_DETAIL_FAILED_CODE);
        assertBizCode(submit(generateNumber("SR"), "8", 100, 100, incomeItemId, null, null),
                ExceptionConstants.ACCOUNT_HEAD_STATUS_FAILED_CODE);

        String billNo = generateNumber("SR");
        Long headId = null;
        try {
            assertSuccess(submit(billNo, "0", 100, 100, incomeItemId, null, null));
            headId = getHeadId(billNo);
            assertBizCode(setStatus(headId, "8"), ExceptionConstants.ACCOUNT_HEAD_STATUS_FAILED_CODE);
        } finally {
            if (headId != null) {
                assertSuccess(tokenReq().param("id", headId).delete(CONTEXT + "/accountHead/delete"));
            }
        }
    }

    @Test
    @DisplayName("拒绝把其它财务单据伪装成收入单更新")
    void rejectCrossTypeUpdate() {
        assumeTrue(hasBaseData(), "缺少收入单测试基础数据");
        String billNo = generateNumber("SR");
        Long headId = null;
        try {
            assertSuccess(submit(billNo, "0", 100, 100, incomeItemId, null, null));
            headId = getHeadId(billNo);
            JSONObject request = buildBody(billNo, "0", 100, 100, incomeItemId, null, null);
            JSONObject info = JSONObject.parseObject(request.getString("info"));
            info.put("id", headId);
            info.put("type", "支出");
            request.put("info", info.toJSONString());
            Response response = tokenReq().body(request.toJSONString())
                    .put(CONTEXT + "/accountHead/updateAccountHeadAndDetail");
            assertBizCode(response, ExceptionConstants.ACCOUNT_HEAD_TYPE_FAILED_CODE);
        } finally {
            if (headId != null) {
                assertSuccess(tokenReq().param("id", headId).delete(CONTEXT + "/accountHead/delete"));
            }
        }
    }

    private static boolean hasBaseData() {
        return jshToken != null && accountId != null && incomeItemId != null;
    }

    private static JSONArray getItems(String type) {
        Response response = tokenGet().param("type", type).get(CONTEXT + "/inOutItem/findBySelect");
        String raw = response.body().asString();
        return raw.startsWith("[") ? JSONArray.parseArray(raw) : null;
    }

    private Response submit(String billNo, String status, double totalPrice, double changeAmount,
                            Long itemId, Long detailAccountId, String billNumber) {
        return tokenReq().body(buildBody(billNo, status, totalPrice, changeAmount,
                        itemId, detailAccountId, billNumber).toJSONString())
                .post(CONTEXT + "/accountHead/addAccountHeadAndDetail");
    }

    private Response update(Long id, String billNo, String status, double totalPrice, double changeAmount,
                            Long itemId, Long detailAccountId) {
        JSONObject body = buildBody(billNo, status, totalPrice, changeAmount, itemId, detailAccountId, null);
        JSONObject info = JSONObject.parseObject(body.getString("info"));
        info.put("id", id);
        body.put("info", info.toJSONString());
        return tokenReq().body(body.toJSONString()).put(CONTEXT + "/accountHead/updateAccountHeadAndDetail");
    }

    private JSONObject buildBody(String billNo, String status, double totalPrice, double changeAmount,
                                 Long itemId, Long detailAccountId, String billNumber) {
        JSONObject info = new JSONObject();
        info.put("type", "收入");
        info.put("billNo", billNo);
        info.put("billTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        info.put("accountId", accountId);
        info.put("totalPrice", totalPrice);
        info.put("changeAmount", changeAmount);
        info.put("discountMoney", 0);
        info.put("status", status);
        info.put("remark", "收入单自动化测试");

        JSONObject detail = new JSONObject();
        detail.put("inOutItemId", itemId);
        detail.put("eachAmount", totalPrice);
        detail.put("remark", "收入项目自动化测试");
        if (detailAccountId != null) {
            detail.put("accountId", detailAccountId);
        }
        if (billNumber != null) {
            detail.put("billNumber", billNumber);
        }
        JSONArray rows = new JSONArray();
        rows.add(detail);

        JSONObject body = new JSONObject();
        body.put("info", info.toJSONString());
        body.put("rows", rows.toJSONString());
        return body;
    }

    private Long getHeadId(String billNo) {
        Response response = tokenGet().param("billNo", billNo)
                .get(CONTEXT + "/accountHead/getDetailByNumber");
        assertSuccess(response);
        return JSONObject.parseObject(response.body().asString()).getJSONObject("data").getLong("id");
    }

    private JSONArray getDetails(Long headId) {
        Response response = tokenGet().param("headerId", headId)
                .get(CONTEXT + "/accountItem/getDetailList");
        assertSuccess(response);
        return JSONObject.parseObject(response.body().asString()).getJSONObject("data").getJSONArray("rows");
    }

    private Response setStatus(Long headId, String status) {
        JSONObject body = new JSONObject();
        body.put("status", status);
        body.put("ids", String.valueOf(headId));
        return tokenReq().body(body.toJSONString()).post(CONTEXT + "/accountHead/batchSetStatus");
    }

    private static RequestSpecification tokenReq() {
        return RestAssured.given().contentType(ContentType.JSON).header("X-Access-Token", jshToken);
    }

    private static RequestSpecification tokenGet() {
        return RestAssured.given().header("X-Access-Token", jshToken);
    }

    private void assertBizCode(Response response, int expectedCode) {
        assertBizError(response);
        assertEquals(expectedCode, JSONObject.parseObject(response.body().asString()).getIntValue("code"),
                response.body().asString());
    }
}
