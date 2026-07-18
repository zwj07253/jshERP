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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("P0: 收款单账务与欠款校验")
public class P0MoneyInValidationTest extends ApiTestBase {

    private static final Long CUSTOMER_ID = 107L;
    private static final String SALES_BILL_NO = "XS-20260709-001";
    private static String jshToken;
    private static Long accountId;

    @BeforeAll
    static void prepareMoneyInData() {
        jshToken = login("jsh", md5("123456"));
        if(jshToken == null) {
            return;
        }
        Response response = tokenGet().get(CONTEXT + "/account/getAccount");
        JSONObject body = JSONObject.parseObject(response.body().asString());
        if(body.getIntValue("code") == 200) {
            JSONArray accounts = body.getJSONObject("data").getJSONArray("accountList");
            if(accounts != null && !accounts.isEmpty()) {
                accountId = accounts.getJSONObject(0).getLong("id");
            }
        }
    }

    @Test
    @DisplayName("草稿不占用欠款，审核和反审核会实时重算欠款")
    void draftAuditAndUnAuditRefreshDebt() {
        JSONObject debtBefore = getDebtBill(CUSTOMER_ID, SALES_BILL_NO);
        assumeTrue(hasBaseData() && debtBefore != null, "缺少收款单测试数据，请先导入 watch_test_data.sql");
        BigDecimal originalDebt = debtBefore.getBigDecimal("debt");
        assumeTrue(originalDebt != null && originalDebt.compareTo(BigDecimal.ONE) > 0,
                "销售出库单没有足够的待收欠款");
        BigDecimal receiptAmount = BigDecimal.ONE;
        String billNo = generateNumber("SK");
        Long headId = null;
        boolean audited = false;
        try {
            assertSuccess(submit(billNo, "0", CUSTOMER_ID, SALES_BILL_NO,
                    receiptAmount, BigDecimal.ZERO, null, null));
            headId = getHeadId(billNo);
            assertNotNull(headId);

            JSONArray details = getDetails(headId);
            assertEquals(1, details.size());
            JSONObject detail = details.getJSONObject(0);
            assertNull(detail.get("accountId"), "收款账户只能保存在主表");
            assertNull(detail.get("inOutItemId"), "收款明细不能混入收支项目");
            assertEquals(0, debtBefore.getBigDecimal("needDebt").compareTo(detail.getBigDecimal("needDebt")));

            assertEquals(0, originalDebt.compareTo(getDebtBill(CUSTOMER_ID, SALES_BILL_NO).getBigDecimal("debt")),
                    "未审核收款单不应占用欠款");

            assertSuccess(setStatus(headId, "1"));
            audited = true;
            JSONObject debtAfterAudit = getDebtBill(CUSTOMER_ID, SALES_BILL_NO);
            assertNotNull(debtAfterAudit);
            assertEquals(0, originalDebt.subtract(receiptAmount).compareTo(debtAfterAudit.getBigDecimal("debt")),
                    "审核后应减少待收欠款");

            assertSuccess(setStatus(headId, "0"));
            audited = false;
            assertEquals(0, originalDebt.compareTo(getDebtBill(CUSTOMER_ID, SALES_BILL_NO).getBigDecimal("debt")),
                    "反审核后应恢复待收欠款");
        } finally {
            if(headId != null) {
                if(audited) {
                    assertSuccess(setStatus(headId, "0"));
                }
                assertSuccess(tokenReq().param("id", headId).delete(CONTEXT + "/accountHead/delete"));
            }
        }
    }

    @Test
    @DisplayName("拒绝超额、负数、主明细不一致和污染明细账户")
    void rejectForgedMoneyInAmounts() {
        JSONObject debtBill = getDebtBill(CUSTOMER_ID, SALES_BILL_NO);
        assumeTrue(hasBaseData() && debtBill != null, "缺少收款单测试数据，请先导入 watch_test_data.sql");
        BigDecimal debt = debtBill.getBigDecimal("debt");

        assertBizCode(submit(generateNumber("SK"), "0", CUSTOMER_ID, SALES_BILL_NO,
                        debt.add(BigDecimal.ONE), BigDecimal.ZERO, null, null),
                ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_AMOUNT_FAILED_CODE);
        assertBizCode(submit(generateNumber("SK"), "0", CUSTOMER_ID, SALES_BILL_NO,
                        BigDecimal.ONE.negate(), BigDecimal.ZERO, null, null),
                ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_AMOUNT_FAILED_CODE);
        assertBizCode(submit(generateNumber("SK"), "0", CUSTOMER_ID, SALES_BILL_NO,
                        BigDecimal.ONE, BigDecimal.ZERO, accountId, null),
                ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_DETAIL_FAILED_CODE);
        assertBizCode(submit(generateNumber("SK"), "0", CUSTOMER_ID, SALES_BILL_NO,
                        BigDecimal.ONE, BigDecimal.ONE, null, BigDecimal.ONE),
                ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_AMOUNT_FAILED_CODE);
    }

    @Test
    @DisplayName("欠款列表严格按照出库和销售类型过滤")
    void debtListHonorsTypeFilters() {
        assumeTrue(hasBaseData(), "缺少收款单测试基础数据");
        JSONArray rows = getDebtRows(CUSTOMER_ID);
        assumeTrue(rows != null && !rows.isEmpty(), "没有可测试的销售欠款单据");
        for(Object value : rows) {
            JSONObject row = JSONObject.parseObject(value.toString());
            assertEquals("出库", row.getString("type"));
            assertEquals("销售", row.getString("subType"));
            assertEquals("1", row.getString("status"));
        }
    }

    private static boolean hasBaseData() {
        return jshToken != null && accountId != null;
    }

    private Response submit(String billNo, String status, Long customerId, String salesBillNo,
                            BigDecimal eachAmount, BigDecimal discountMoney, Long detailAccountId,
                            BigDecimal forcedChangeAmount) {
        JSONObject info = new JSONObject();
        info.put("type", "收款");
        info.put("organId", customerId);
        info.put("billNo", billNo);
        info.put("billTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        info.put("accountId", accountId);
        info.put("totalPrice", eachAmount);
        info.put("discountMoney", discountMoney);
        info.put("changeAmount", forcedChangeAmount == null ? eachAmount.subtract(discountMoney) : forcedChangeAmount);
        info.put("status", status);
        info.put("remark", "收款单自动化测试");

        JSONObject detail = new JSONObject();
        detail.put("billNumber", salesBillNo);
        detail.put("needDebt", new BigDecimal("999999"));
        detail.put("finishDebt", new BigDecimal("999999"));
        detail.put("eachAmount", eachAmount);
        detail.put("remark", "收款明细自动化测试");
        if(detailAccountId != null) {
            detail.put("accountId", detailAccountId);
        }
        JSONArray rows = new JSONArray();
        rows.add(detail);

        JSONObject body = new JSONObject();
        body.put("info", info.toJSONString());
        body.put("rows", rows.toJSONString());
        return tokenReq().body(body.toJSONString()).post(CONTEXT + "/accountHead/addAccountHeadAndDetail");
    }

    private JSONObject getDebtBill(Long customerId, String billNo) {
        JSONArray rows = getDebtRows(customerId);
        if(rows != null) {
            for(Object value : rows) {
                JSONObject row = JSONObject.parseObject(value.toString());
                if(billNo.equals(row.getString("number"))) {
                    return row;
                }
            }
        }
        return null;
    }

    private JSONArray getDebtRows(Long customerId) {
        JSONObject search = new JSONObject();
        search.put("organId", String.valueOf(customerId));
        search.put("type", "出库");
        search.put("subType", "销售");
        Response response = tokenGet()
                .param("search", search.toJSONString())
                .param("currentPage", 1)
                .param("pageSize", 100)
                .get(CONTEXT + "/depotHead/debtList");
        if(JSONObject.parseObject(response.body().asString()).getIntValue("code") != 200) {
            return null;
        }
        return JSONObject.parseObject(response.body().asString()).getJSONObject("data").getJSONArray("rows");
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
