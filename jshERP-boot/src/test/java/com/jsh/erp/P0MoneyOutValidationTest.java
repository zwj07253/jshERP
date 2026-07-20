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

@DisplayName("P0: 付款单账务与欠款校验")
public class P0MoneyOutValidationTest extends ApiTestBase {

    private static final Long SUPPLIER_ID = 101L;
    private static final Long OTHER_SUPPLIER_ID = 102L;
    private static final Long OPENING_SUPPLIER_ID = 104L;
    private static final String PURCHASE_BILL_NO = "PO-20260510-001";
    private static String jshToken;
    private static Long accountId;

    @BeforeAll
    static void prepareMoneyOutData() {
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
    @DisplayName("草稿不占用欠款，审核和反审核会实时重算应付欠款")
    void draftAuditAndUnAuditRefreshDebt() {
        JSONObject debtBefore = getDebtBill(SUPPLIER_ID, PURCHASE_BILL_NO);
        assumeTrue(hasBaseData() && debtBefore != null, "请先导入修正后的 watch_test_data.sql");
        BigDecimal originalDebt = debtBefore.getBigDecimal("debt");
        assumeTrue(originalDebt != null && originalDebt.compareTo(BigDecimal.ONE) > 0,
                "采购入库单没有足够的待付欠款");
        BigDecimal paymentAmount = BigDecimal.ONE;
        String billNo = generateNumber("FK");
        Long headId = null;
        boolean audited = false;
        try {
            assertSuccess(submit(billNo, "0", SUPPLIER_ID, PURCHASE_BILL_NO,
                    paymentAmount, BigDecimal.ZERO, null, null));
            headId = getHeadId(billNo);
            assertNotNull(headId);

            JSONArray details = getDetails(headId);
            assertEquals(1, details.size());
            JSONObject detail = details.getJSONObject(0);
            assertNull(detail.get("accountId"), "付款账户只能保存在主表");
            assertNull(detail.get("inOutItemId"), "付款明细不能混入收支项目");
            assertEquals(0, debtBefore.getBigDecimal("needDebt").compareTo(detail.getBigDecimal("needDebt")));
            assertEquals(0, originalDebt.compareTo(getDebtBill(SUPPLIER_ID, PURCHASE_BILL_NO).getBigDecimal("debt")),
                    "未审核付款单不应占用欠款");

            assertSuccess(setStatus(headId, "1"));
            audited = true;
            assertEquals(0, originalDebt.subtract(paymentAmount)
                            .compareTo(getDebtBill(SUPPLIER_ID, PURCHASE_BILL_NO).getBigDecimal("debt")),
                    "审核后应减少待付欠款");

            assertSuccess(setStatus(headId, "0"));
            audited = false;
            assertEquals(0, originalDebt.compareTo(getDebtBill(SUPPLIER_ID, PURCHASE_BILL_NO).getBigDecimal("debt")),
                    "反审核后应恢复待付欠款");
        } finally {
            cleanup(headId, audited);
        }
    }

    @Test
    @DisplayName("拒绝超额、负数、跨供应商、金额不一致和污染明细")
    void rejectForgedMoneyOutAmounts() {
        JSONObject debtBill = getDebtBill(SUPPLIER_ID, PURCHASE_BILL_NO);
        assumeTrue(hasBaseData() && debtBill != null, "缺少付款单测试数据");
        BigDecimal debt = debtBill.getBigDecimal("debt");

        assertBizCode(submit(generateNumber("FK"), "0", SUPPLIER_ID, PURCHASE_BILL_NO,
                        debt.add(BigDecimal.ONE), BigDecimal.ZERO, null, null),
                ExceptionConstants.ACCOUNT_HEAD_MONEY_OUT_AMOUNT_FAILED_CODE);
        assertBizCode(submit(generateNumber("FK"), "0", SUPPLIER_ID, PURCHASE_BILL_NO,
                        BigDecimal.ONE.negate(), BigDecimal.ZERO, null, null),
                ExceptionConstants.ACCOUNT_HEAD_MONEY_OUT_AMOUNT_FAILED_CODE);
        assertBizCode(submit(generateNumber("FK"), "0", SUPPLIER_ID, PURCHASE_BILL_NO,
                        BigDecimal.ONE, BigDecimal.ZERO, accountId, null),
                ExceptionConstants.ACCOUNT_HEAD_MONEY_OUT_DETAIL_FAILED_CODE);
        assertBizCode(submit(generateNumber("FK"), "0", OTHER_SUPPLIER_ID, PURCHASE_BILL_NO,
                        BigDecimal.ONE, BigDecimal.ZERO, null, null),
                ExceptionConstants.ACCOUNT_HEAD_MONEY_OUT_DETAIL_FAILED_CODE);
        assertBizCode(submit(generateNumber("FK"), "0", SUPPLIER_ID, PURCHASE_BILL_NO,
                        new BigDecimal("2"), BigDecimal.ONE, null, new BigDecimal("-2")),
                ExceptionConstants.ACCOUNT_HEAD_MONEY_OUT_AMOUNT_FAILED_CODE);
    }

    @Test
    @DisplayName("期初应付可以保存为无业务单据关联的付款明细")
    void openingPayableCanBeSaved() {
        assumeTrue(hasBaseData(), "缺少付款单测试基础数据");
        String billNo = generateNumber("FK");
        Long headId = null;
        try {
            assertSuccess(submit(billNo, "0", OPENING_SUPPLIER_ID, "QiChu",
                    BigDecimal.ONE, BigDecimal.ZERO, null, null));
            headId = getHeadId(billNo);
            JSONArray details = getDetails(headId);
            assertEquals(1, details.size());
            assertEquals("QiChu", details.getJSONObject(0).getString("billNumber"));
            assertEquals(0, new BigDecimal("3000").compareTo(details.getJSONObject(0).getBigDecimal("needDebt")));
        } finally {
            cleanup(headId, false);
        }
    }

    @Test
    @DisplayName("待付款列表严格按照入库、采购和审核状态过滤")
    void debtListHonorsTypeFilters() {
        assumeTrue(hasBaseData(), "缺少付款单测试基础数据");
        JSONArray rows = getDebtRows(SUPPLIER_ID);
        assumeTrue(rows != null && !rows.isEmpty(), "没有可测试的采购欠款单据");
        for(Object value : rows) {
            JSONObject row = JSONObject.parseObject(value.toString());
            assertEquals("入库", row.getString("type"));
            assertEquals("采购", row.getString("subType"));
            assertEquals("1", row.getString("status"));
        }
    }

    private boolean hasBaseData() {
        return jshToken != null && accountId != null;
    }

    private Response submit(String billNo, String status, Long supplierId, String purchaseBillNo,
                            BigDecimal eachAmount, BigDecimal discountMoney, Long detailAccountId,
                            BigDecimal forcedChangeAmount) {
        JSONObject info = new JSONObject();
        info.put("type", "付款");
        info.put("organId", supplierId);
        info.put("billNo", billNo);
        info.put("billTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        info.put("accountId", accountId);
        info.put("totalPrice", eachAmount.negate());
        info.put("discountMoney", discountMoney);
        info.put("changeAmount", forcedChangeAmount == null
                ? eachAmount.subtract(discountMoney).negate() : forcedChangeAmount);
        info.put("status", status);
        info.put("remark", "付款单自动化测试");

        JSONObject detail = new JSONObject();
        detail.put("billNumber", purchaseBillNo);
        detail.put("needDebt", new BigDecimal("999999"));
        detail.put("finishDebt", new BigDecimal("999999"));
        detail.put("eachAmount", eachAmount);
        detail.put("remark", "付款明细自动化测试");
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

    private JSONObject getDebtBill(Long supplierId, String billNo) {
        JSONArray rows = getDebtRows(supplierId);
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

    private JSONArray getDebtRows(Long supplierId) {
        JSONObject search = new JSONObject();
        search.put("organId", String.valueOf(supplierId));
        search.put("type", "入库");
        search.put("subType", "采购");
        Response response = tokenGet()
                .param("search", search.toJSONString())
                .param("currentPage", 1)
                .param("pageSize", 100)
                .get(CONTEXT + "/depotHead/debtList");
        JSONObject body = JSONObject.parseObject(response.body().asString());
        return body.getIntValue("code") == 200 ? body.getJSONObject("data").getJSONArray("rows") : null;
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

    private void cleanup(Long headId, boolean audited) {
        if(headId == null) {
            return;
        }
        if(audited) {
            assertSuccess(setStatus(headId, "0"));
        }
        assertSuccess(tokenReq().param("id", headId).delete(CONTEXT + "/accountHead/delete"));
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
