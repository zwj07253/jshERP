package com.jsh.erp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("P0: 收预付款与会员余额一致性")
public class P0AdvanceInValidationTest extends ApiTestBase {

    private static final Long MEMBER_A = 109L;
    private static final Long MEMBER_B = 110L;
    private static final Long NON_MEMBER = 107L;
    private static Long accountId;
    private static String barCode;
    private static String unit;
    private static Long depotId;
    private static BigDecimal retailPrice;

    @BeforeAll
    static void prepareAdvanceInData() {
        P0AdvanceInValidationTest test = new P0AdvanceInValidationTest();
        Response accountResponse = test.authReqGet().get(CONTEXT + "/account/getAccount");
        JSONObject accountBody = JSONObject.parseObject(accountResponse.body().asString());
        if(accountBody.getIntValue("code") == 200) {
            JSONArray accounts = accountBody.getJSONObject("data").getJSONArray("accountList");
            if(accounts != null && !accounts.isEmpty()) {
                accountId = accounts.getJSONObject(0).getLong("id");
            }
        }

        Response materialResponse = test.authReqGet().param("q", "").param("page", 1).param("rows", 10)
                .get(CONTEXT + "/material/findBySelect");
        JSONObject materialBody = JSONObject.parseObject(materialResponse.body().asString());
        if(materialBody.getLongValue("total") > 0) {
            JSONArray materials = materialBody.getJSONArray("rows");
            for(int index = 0; index < materials.size(); index++) {
                JSONObject material = materials.getJSONObject(index);
                if(!"1".equals(material.getString("enableBatchNumber"))
                        && !"1".equals(material.getString("enableSerialNumber"))) {
                    barCode = material.getString("mBarCode");
                    unit = material.getString("unit");
                    if(unit != null) {
                        unit = unit.replaceAll("\\[[^]]*]$", "");
                    }
                    break;
                }
            }
        }

        Response depotResponse = test.authReqGet().param("search", "{}").get(CONTEXT + "/depot/list");
        JSONObject depotData = JSONObject.parseObject(depotResponse.body().asString()).getJSONObject("data");
        if(depotData != null && depotData.getLongValue("total") > 0) {
            depotId = depotData.getJSONArray("rows").getJSONObject(0).getLong("id");
        }
        if(barCode != null && depotId != null) {
            Response priceResponse = test.authReqGet().param("barCode", barCode)
                    .param("organId", MEMBER_A).param("depotId", depotId)
                    .param("mpList", "").param("prefixNo", "LSCK")
                    .get(CONTEXT + "/material/getMaterialByBarCode");
            JSONObject priceBody = JSONObject.parseObject(priceResponse.body().asString());
            JSONArray priceRows = priceBody.getJSONArray("data");
            if(priceBody.getIntValue("code") == 200 && priceRows != null && !priceRows.isEmpty()) {
                retailPrice = priceRows.getJSONObject(0).getBigDecimal("billPrice");
            }
        }
    }

    @Test
    @DisplayName("新增草稿不增加余额，审核增加，反审核恢复")
    void addAuditAndUnauditRefreshBalance() {
        assumeTrue(hasMemberData(), "缺少会员和账户测试数据，请先导入 watch_test_data.sql");
        BigDecimal before = getAdvance(MEMBER_B);
        String billNo = generateNumber("SYF");
        Long headId = null;
        boolean audited = false;
        try {
            assertSuccess(submitAdvance(null, billNo, "0", MEMBER_B, accountId,
                    BigDecimal.ONE, BigDecimal.ONE, null));
            headId = getAccountHeadId(billNo);
            assertNotNull(headId);
            assertMoney(before, getAdvance(MEMBER_B), "草稿不能增加会员余额");

            assertSuccess(setAccountHeadStatus(headId, "1"));
            audited = true;
            assertMoney(before.add(BigDecimal.ONE), getAdvance(MEMBER_B), "审核后应增加会员余额");

            assertSuccess(setAccountHeadStatus(headId, "0"));
            audited = false;
            assertMoney(before, getAdvance(MEMBER_B), "反审核后应恢复会员余额");
        } finally {
            cleanupAccountHead(headId, audited);
        }
    }

    @Test
    @DisplayName("编辑更换会员并审核只更新新会员余额")
    void editAndChangeMemberRefreshesBothMembers() {
        assumeTrue(hasMemberData(), "缺少会员和账户测试数据");
        BigDecimal memberABefore = getAdvance(MEMBER_A);
        BigDecimal memberBBefore = getAdvance(MEMBER_B);
        BigDecimal amount = new BigDecimal("2.00");
        String billNo = generateNumber("SYF");
        Long headId = null;
        boolean audited = false;
        try {
            assertSuccess(submitAdvance(null, billNo, "0", MEMBER_A, accountId, amount, amount, null));
            headId = getAccountHeadId(billNo);
            assertSuccess(submitAdvance(headId, billNo, "1", MEMBER_B, accountId, amount, amount, null));
            audited = true;

            assertMoney(memberABefore, getAdvance(MEMBER_A), "旧会员余额不应残留");
            assertMoney(memberBBefore.add(amount), getAdvance(MEMBER_B), "新会员余额应增加");
        } finally {
            cleanupAccountHead(headId, audited);
        }
        assertMoney(memberABefore, getAdvance(MEMBER_A), "清理后旧会员余额应恢复");
        assertMoney(memberBBefore, getAdvance(MEMBER_B), "清理后新会员余额应恢复");
    }

    @Test
    @DisplayName("拒绝负数金额、非会员、禁用账户和污染明细")
    void rejectInvalidAdvanceInData() {
        assumeTrue(hasMemberData(), "缺少会员和账户测试数据");
        assertBizCode(submitAdvance(null, generateNumber("SYF"), "0", MEMBER_A, accountId,
                        BigDecimal.ONE.negate(), BigDecimal.ONE.negate(), null),
                ExceptionConstants.ACCOUNT_HEAD_ADVANCE_IN_DETAIL_FAILED_CODE);
        assertBizCode(submitAdvance(null, generateNumber("SYF"), "0", NON_MEMBER, accountId,
                        BigDecimal.ONE, BigDecimal.ONE, null),
                ExceptionConstants.ACCOUNT_HEAD_ADVANCE_IN_ORGAN_FAILED_CODE);

        try {
            assertSuccess(setAccountStatus(accountId, false));
            assertBizCode(submitAdvance(null, generateNumber("SYF"), "0", MEMBER_A, accountId,
                            BigDecimal.ONE, BigDecimal.ONE, null),
                    ExceptionConstants.ACCOUNT_HEAD_ADVANCE_IN_ACCOUNT_FAILED_CODE);
        } finally {
            assertSuccess(setAccountStatus(accountId, true));
        }

        JSONObject polluted = new JSONObject();
        polluted.put("billNumber", "XS-20260709-001");
        polluted.put("needDebt", 999999);
        assertBizCode(submitAdvance(null, generateNumber("SYF"), "0", MEMBER_A, accountId,
                        BigDecimal.ONE, BigDecimal.ONE, polluted),
                ExceptionConstants.ACCOUNT_HEAD_ADVANCE_IN_DETAIL_FAILED_CODE);
    }

    @Test
    @DisplayName("预付款零售出库仅在审核后抵扣，反审核后恢复")
    void prepaidRetailDeductsOnlyAfterAudit() {
        assumeTrue(hasRetailData(), "缺少会员、商品、仓库或账户测试数据");
        BigDecimal before = getAdvance(MEMBER_A);
        assumeTrue(retailPrice != null && retailPrice.compareTo(BigDecimal.ZERO) > 0
                && before.compareTo(retailPrice) >= 0, "会员预付款余额不足以执行抵扣测试");
        String number = generateNumber("LSCK");
        Long headId = null;
        boolean audited = false;
        try {
            assertSuccess(submitPrepaidRetail(number));
            headId = getDepotHeadId(number);
            assertNotNull(headId);
            assertMoney(before, getAdvance(MEMBER_A), "零售草稿不能扣减预付款");

            auditDepotHead(headId);
            audited = true;
            assertMoney(before.subtract(retailPrice), getAdvance(MEMBER_A), "审核后应扣减预付款");

            unauditDepotHead(headId);
            audited = false;
            assertMoney(before, getAdvance(MEMBER_A), "反审核后应恢复预付款");
        } finally {
            if(headId != null) {
                if(audited) {
                    unauditDepotHead(headId);
                }
                assertSuccess(authReq().param("id", headId).delete(CONTEXT + "/depotHead/delete"));
            }
        }
    }

    @Test
    @DisplayName("并发新增已审核预付款不会丢失会员余额")
    void concurrentReceiptsKeepCachedBalanceConsistent() throws Exception {
        assumeTrue(hasMemberData(), "缺少会员和账户测试数据");
        BigDecimal before = getAdvance(MEMBER_B);
        String firstBillNo = generateNumber("SYF");
        String secondBillNo = generateNumber("SYF");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Long> headIds = new ArrayList<>();
        try {
            Future<Response> first = executor.submit(() -> concurrentSubmit(firstBillNo, ready, start));
            Future<Response> second = executor.submit(() -> concurrentSubmit(secondBillNo, ready, start));
            ready.await();
            start.countDown();
            assertSuccess(first.get());
            assertSuccess(second.get());
            headIds.add(getAccountHeadId(firstBillNo));
            headIds.add(getAccountHeadId(secondBillNo));
            assertMoney(before.add(new BigDecimal("2.00")), getAdvance(MEMBER_B),
                    "并发收款后缓存余额必须等于已审核流水合计");
        } finally {
            start.countDown();
            executor.shutdownNow();
            for(Long headId : headIds) {
                cleanupAccountHead(headId, true);
            }
        }
        assertMoney(before, getAdvance(MEMBER_B), "清理并发测试单据后余额应恢复");
    }

    private Response concurrentSubmit(String billNo, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        return submitAdvance(null, billNo, "1", MEMBER_B, accountId,
                BigDecimal.ONE, BigDecimal.ONE, null);
    }

    private Response submitAdvance(Long id, String billNo, String status, Long memberId, Long detailAccountId,
                                   BigDecimal totalPrice, BigDecimal eachAmount, JSONObject extraDetail) {
        JSONObject info = new JSONObject();
        if(id != null) {
            info.put("id", id);
        }
        info.put("type", "收预付款");
        info.put("organId", memberId);
        info.put("billNo", billNo);
        info.put("billTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        info.put("totalPrice", totalPrice);
        info.put("changeAmount", totalPrice);
        info.put("discountMoney", BigDecimal.ZERO);
        info.put("status", status);
        info.put("remark", "收预付款自动化测试");

        JSONObject detail = extraDetail == null ? new JSONObject() : extraDetail.clone();
        detail.put("accountId", detailAccountId);
        detail.put("eachAmount", eachAmount);
        detail.put("remark", "收预付款明细自动化测试");
        JSONArray rows = new JSONArray();
        rows.add(detail);

        JSONObject body = new JSONObject();
        body.put("info", info.toJSONString());
        body.put("rows", rows.toJSONString());
        String url = id == null ? "/accountHead/addAccountHeadAndDetail"
                : "/accountHead/updateAccountHeadAndDetail";
        return id == null
                ? authReq().body(body.toJSONString()).post(CONTEXT + url)
                : authReq().body(body.toJSONString()).put(CONTEXT + url);
    }

    private Response submitPrepaidRetail(String number) {
        JSONObject info = new JSONObject();
        info.put("number", number);
        info.put("type", "出库");
        info.put("subType", "零售");
        info.put("organId", MEMBER_A);
        info.put("accountId", accountId);
        info.put("payType", "预付款");
        info.put("getAmount", retailPrice);
        info.put("backAmount", BigDecimal.ZERO);
        info.put("changeAmount", retailPrice);
        info.put("totalPrice", retailPrice);
        info.put("status", "0");

        JSONObject detail = new JSONObject();
        detail.put("barCode", barCode);
        detail.put("depotId", depotId);
        detail.put("unit", unit == null ? "" : unit);
        detail.put("operNumber", BigDecimal.ONE);
        detail.put("unitPrice", retailPrice);
        detail.put("allPrice", retailPrice);
        JSONArray rows = new JSONArray();
        rows.add(detail);

        JSONObject body = new JSONObject();
        body.put("info", info.toJSONString());
        body.put("rows", rows.toJSONString());
        return authReq().body(body.toJSONString()).post(CONTEXT + "/depotHead/addDepotHeadAndDetail");
    }

    private BigDecimal getAdvance(Long memberId) {
        Response response = authReqGet().param("id", memberId).get(CONTEXT + "/supplier/info");
        assertSuccess(response);
        return JSONObject.parseObject(response.body().asString()).getJSONObject("data")
                .getJSONObject("info").getBigDecimal("advanceIn");
    }

    private Long getAccountHeadId(String billNo) {
        Response response = authReqGet().param("billNo", billNo)
                .get(CONTEXT + "/accountHead/getDetailByNumber");
        assertSuccess(response);
        return JSONObject.parseObject(response.body().asString()).getJSONObject("data").getLong("id");
    }

    private Long getDepotHeadId(String number) {
        Response response = authReqGet().param("number", number)
                .get(CONTEXT + "/depotHead/getDetailByNumber");
        assertSuccess(response);
        return JSONObject.parseObject(response.body().asString()).getJSONObject("data").getLong("id");
    }

    private Response setAccountHeadStatus(Long headId, String status) {
        JSONObject body = new JSONObject();
        body.put("status", status);
        body.put("ids", String.valueOf(headId));
        return authReq().body(body.toJSONString()).post(CONTEXT + "/accountHead/batchSetStatus");
    }

    private Response setAccountStatus(Long id, boolean status) {
        JSONObject body = new JSONObject();
        body.put("status", status);
        body.put("ids", String.valueOf(id));
        return authReq().body(body.toJSONString()).post(CONTEXT + "/account/batchSetStatus");
    }

    private void cleanupAccountHead(Long headId, boolean audited) {
        if(headId == null) {
            return;
        }
        if(audited) {
            assertSuccess(setAccountHeadStatus(headId, "0"));
        }
        assertSuccess(authReq().param("id", headId).delete(CONTEXT + "/accountHead/delete"));
    }

    private void assertBizCode(Response response, int expectedCode) {
        assertBizError(response);
        assertEquals(expectedCode, JSONObject.parseObject(response.body().asString()).getIntValue("code"),
                response.body().asString());
    }

    private void assertMoney(BigDecimal expected, BigDecimal actual, String message) {
        assertNotNull(actual, message);
        assertEquals(0, expected.compareTo(actual), message + "，expected=" + expected + ", actual=" + actual);
    }

    private boolean hasMemberData() {
        return accountId != null && memberExists(MEMBER_A) && memberExists(MEMBER_B);
    }

    private boolean hasRetailData() {
        return hasMemberData() && barCode != null && depotId != null && retailPrice != null;
    }

    private boolean memberExists(Long id) {
        Response response = authReqGet().param("id", id).get(CONTEXT + "/supplier/info");
        JSONObject body = JSONObject.parseObject(response.body().asString());
        return body.getIntValue("code") == 200 && body.getJSONObject("data") != null
                && body.getJSONObject("data").getJSONObject("info") != null;
    }
}
