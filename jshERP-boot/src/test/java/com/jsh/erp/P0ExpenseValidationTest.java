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

@DisplayName("P0: 支出单权限与账务校验")
public class P0ExpenseValidationTest extends ApiTestBase {

    private static String jshToken;
    private static Long accountId;
    private static Long expenseItemId;
    private static Long incomeItemId;

    @BeforeAll
    static void prepareExpenseData() {
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
        JSONArray expenseItems = getItems("out");
        if (expenseItems != null && !expenseItems.isEmpty()) {
            expenseItemId = expenseItems.getJSONObject(0).getLong("id");
        }
        JSONArray incomeItems = getItems("in");
        if (incomeItems != null && !incomeItems.isEmpty()) {
            incomeItemId = incomeItems.getJSONObject(0).getLong("id");
        }
    }

    @Test
    @DisplayName("支出单以负数记主表账户并保护已审核单据")
    void saveAuditAndProtectAuditedBill() {
        assumeTrue(hasBaseData(), "缺少支出单测试基础数据");
        String billNo = generateNumber("ZC");
        Long headId = null;
        boolean audited = false;
        try {
            assertSuccess(submit(billNo, "0", -100, -100, 100,
                    expenseItemId, null, null, accountId));
            headId = getHeadId(billNo);
            assertNotNull(headId);

            JSONObject head = getHead(headId);
            assertEquals(0, head.getBigDecimal("totalPrice").compareTo(new java.math.BigDecimal("-100")));
            assertEquals(0, head.getBigDecimal("changeAmount").compareTo(new java.math.BigDecimal("-100")));
            JSONArray details = getDetails(headId);
            assertEquals(1, details.size());
            assertNull(details.getJSONObject(0).get("accountId"), "支出账户只能保存在主表，不能重复写入明细");

            assertSuccess(setStatus(headId, "1"));
            audited = true;
            assertBizCode(tokenReq().param("id", headId).delete(CONTEXT + "/accountHead/delete"),
                    ExceptionConstants.ACCOUNT_HEAD_UN_AUDIT_DELETE_FAILED_CODE);
            Response updateResponse = update(headId, billNo, "0", -100, -100, 100,
                    expenseItemId, null);
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
    @DisplayName("拒绝错误符号、金额不一致、非法项目、污染明细和无效账户")
    void rejectForgedExpenseData() {
        assumeTrue(hasBaseData() && incomeItemId != null, "缺少支出单测试基础数据");
        assertBizCode(submit(generateNumber("ZC"), "0", 100, 100, 100,
                        expenseItemId, null, null, accountId),
                ExceptionConstants.ACCOUNT_HEAD_EXPENSE_AMOUNT_FAILED_CODE);
        assertBizCode(submit(generateNumber("ZC"), "0", -100, -99, 100,
                        expenseItemId, null, null, accountId),
                ExceptionConstants.ACCOUNT_HEAD_EXPENSE_AMOUNT_FAILED_CODE);
        assertBizCode(submit(generateNumber("ZC"), "0", 1, 1, -1,
                        expenseItemId, null, null, accountId),
                ExceptionConstants.ACCOUNT_HEAD_EXPENSE_DETAIL_FAILED_CODE);
        assertBizCode(submit(generateNumber("ZC"), "0", -100, -100, 100,
                        incomeItemId, null, null, accountId),
                ExceptionConstants.ACCOUNT_HEAD_EXPENSE_DETAIL_FAILED_CODE);
        assertBizCode(submit(generateNumber("ZC"), "0", -100, -100, 100,
                        expenseItemId, accountId, null, accountId),
                ExceptionConstants.ACCOUNT_HEAD_EXPENSE_DETAIL_FAILED_CODE);
        assertBizCode(submit(generateNumber("ZC"), "0", -100, -100, 100,
                        expenseItemId, null, "QiChu", accountId),
                ExceptionConstants.ACCOUNT_HEAD_EXPENSE_DETAIL_FAILED_CODE);
        assertBizCode(submit(generateNumber("ZC"), "0", -100, -100, 100,
                        expenseItemId, null, null, Long.MAX_VALUE),
                ExceptionConstants.ACCOUNT_HEAD_EXPENSE_ACCOUNT_FAILED_CODE);
        assertBizCode(submit(generateNumber("ZC"), "8", -100, -100, 100,
                        expenseItemId, null, null, accountId),
                ExceptionConstants.ACCOUNT_HEAD_STATUS_FAILED_CODE);
    }

    private static boolean hasBaseData() {
        return jshToken != null && accountId != null && expenseItemId != null;
    }

    private static JSONArray getItems(String type) {
        Response response = tokenGet().param("type", type).get(CONTEXT + "/inOutItem/findBySelect");
        String raw = response.body().asString();
        return raw.startsWith("[") ? JSONArray.parseArray(raw) : null;
    }

    private Response submit(String billNo, String status, double totalPrice, double changeAmount,
                            double detailAmount, Long itemId, Long detailAccountId,
                            String billNumber, Long mainAccountId) {
        return tokenReq().body(buildBody(billNo, status, totalPrice, changeAmount, detailAmount,
                        itemId, detailAccountId, billNumber, mainAccountId).toJSONString())
                .post(CONTEXT + "/accountHead/addAccountHeadAndDetail");
    }

    private Response update(Long id, String billNo, String status, double totalPrice, double changeAmount,
                            double detailAmount, Long itemId, Long detailAccountId) {
        JSONObject body = buildBody(billNo, status, totalPrice, changeAmount, detailAmount,
                itemId, detailAccountId, null, accountId);
        JSONObject info = JSONObject.parseObject(body.getString("info"));
        info.put("id", id);
        body.put("info", info.toJSONString());
        return tokenReq().body(body.toJSONString()).put(CONTEXT + "/accountHead/updateAccountHeadAndDetail");
    }

    private JSONObject buildBody(String billNo, String status, double totalPrice, double changeAmount,
                                 double detailAmount, Long itemId, Long detailAccountId,
                                 String billNumber, Long mainAccountId) {
        JSONObject info = new JSONObject();
        info.put("type", "支出");
        info.put("billNo", billNo);
        info.put("billTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        info.put("accountId", mainAccountId);
        info.put("totalPrice", totalPrice);
        info.put("changeAmount", changeAmount);
        info.put("discountMoney", 0);
        info.put("status", status);
        info.put("remark", "支出单自动化测试");

        JSONObject detail = new JSONObject();
        detail.put("inOutItemId", itemId);
        detail.put("eachAmount", detailAmount);
        detail.put("remark", "支出项目自动化测试");
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

    private JSONObject getHead(Long headId) {
        Response response = tokenGet().param("id", headId).get(CONTEXT + "/accountHead/info");
        assertSuccess(response);
        return JSONObject.parseObject(response.body().asString()).getJSONObject("data").getJSONObject("info");
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
