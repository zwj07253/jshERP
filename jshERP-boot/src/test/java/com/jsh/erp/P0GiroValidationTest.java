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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("P0: 转账单资金守恒与账户校验")
public class P0GiroValidationTest extends ApiTestBase {

    private static String jshToken;
    private static Long sourceAccountId;
    private static Long targetAccountId;
    private static Long secondTargetAccountId;

    @BeforeAll
    static void prepareGiroData() {
        jshToken = login("jsh", md5("123456"));
        if(jshToken == null) {
            return;
        }
        Response response = tokenGet().get(CONTEXT + "/account/getAccount");
        JSONObject body = JSONObject.parseObject(response.body().asString());
        if(body.getIntValue("code") == 200) {
            JSONArray accounts = body.getJSONObject("data").getJSONArray("accountList");
            if(accounts != null && accounts.size() >= 3) {
                sourceAccountId = accounts.getJSONObject(0).getLong("id");
                targetAccountId = accounts.getJSONObject(1).getLong("id");
                secondTargetAccountId = accounts.getJSONObject(2).getLong("id");
            }
        }
    }

    @Test
    @DisplayName("一对多转账审核后资金净变化为零")
    void auditedGiroKeepsMoneyBalanced() throws Exception {
        assumeTrue(hasBaseData(), "请先导入 watch_test_data.sql");
        String billNo = billNo();
        Long headId = null;
        boolean audited = false;
        try {
            List<JSONObject> details = new ArrayList<>();
            details.add(detail(targetAccountId, new BigDecimal("1.25")));
            details.add(detail(secondTargetAccountId, new BigDecimal("2.75")));
            assertSuccess(submit(null, billNo, "1", sourceAccountId,
                    new BigDecimal("-4.00"), new BigDecimal("-4.00"), null, details));
            headId = getHeadId(billNo);
            audited = true;
            assertNotNull(headId);
            assertEquals(0, queryNetDelta(headId).compareTo(BigDecimal.ZERO),
                    "转出金额与全部转入金额之和必须为零");

            JSONArray storedDetails = getDetails(headId);
            assertEquals(2, storedDetails.size());
            for(Object value : storedDetails) {
                JSONObject stored = JSONObject.parseObject(value.toString());
                assertNull(stored.get("inOutItemId"));
                assertNull(stored.get("needDebt"));
                assertNull(stored.get("finishDebt"));
            }
        } finally {
            cleanup(headId, audited);
        }
    }

    @Test
    @DisplayName("拒绝金额不一致、零负数、非法账户和污染字段")
    void rejectForgedGiroData() throws Exception {
        assumeTrue(hasBaseData(), "缺少转账单测试账户");
        assertBizCode(singleTransfer(sourceAccountId, targetAccountId, BigDecimal.ONE,
                        new BigDecimal("-2"), BigDecimal.ONE.negate(), null),
                ExceptionConstants.ACCOUNT_HEAD_GIRO_AMOUNT_FAILED_CODE);
        assertBizCode(singleTransfer(sourceAccountId, targetAccountId, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, null),
                ExceptionConstants.ACCOUNT_HEAD_GIRO_AMOUNT_FAILED_CODE);
        assertBizCode(singleTransfer(sourceAccountId, targetAccountId, BigDecimal.ONE.negate(),
                        BigDecimal.ONE, BigDecimal.ONE, null),
                ExceptionConstants.ACCOUNT_HEAD_GIRO_AMOUNT_FAILED_CODE);
        assertBizCode(singleTransfer(Long.MAX_VALUE, targetAccountId, BigDecimal.ONE,
                        BigDecimal.ONE.negate(), BigDecimal.ONE.negate(), null),
                ExceptionConstants.ACCOUNT_HEAD_GIRO_ACCOUNT_FAILED_CODE);
        assertBizCode(singleTransfer(sourceAccountId, Long.MAX_VALUE, BigDecimal.ONE,
                        BigDecimal.ONE.negate(), BigDecimal.ONE.negate(), null),
                ExceptionConstants.ACCOUNT_HEAD_GIRO_DETAIL_FAILED_CODE);

        try {
            setAccountEnabled(targetAccountId, false);
            assertBizCode(singleTransfer(sourceAccountId, targetAccountId, BigDecimal.ONE,
                            BigDecimal.ONE.negate(), BigDecimal.ONE.negate(), null),
                    ExceptionConstants.ACCOUNT_HEAD_GIRO_DETAIL_FAILED_CODE);
        } finally {
            setAccountEnabled(targetAccountId, true);
        }

        JSONObject polluted = detail(targetAccountId, BigDecimal.ONE);
        polluted.put("billNumber", "PO-20260510-001");
        polluted.put("inOutItemId", 101L);
        assertBizCode(submit(null, billNo(), "0", sourceAccountId, BigDecimal.ONE.negate(),
                        BigDecimal.ONE.negate(), null, List.of(polluted)),
                ExceptionConstants.ACCOUNT_HEAD_GIRO_DETAIL_FAILED_CODE);
    }

    @Test
    @DisplayName("新增和编辑均拒绝转入转出账户相同")
    void rejectSameAccountOnCreateAndEdit() {
        assumeTrue(hasBaseData(), "缺少转账单测试账户");
        assertBizCode(singleTransfer(sourceAccountId, sourceAccountId, BigDecimal.ONE,
                        BigDecimal.ONE.negate(), BigDecimal.ONE.negate(), null),
                ExceptionConstants.ACCOUNT_HEAD_ACCOUNT_REPEAT_CODE);

        String billNo = billNo();
        Long headId = null;
        try {
            assertSuccess(singleTransfer(sourceAccountId, targetAccountId, BigDecimal.ONE,
                    BigDecimal.ONE.negate(), BigDecimal.ONE.negate(), billNo));
            headId = getHeadId(billNo);
            assertBizCode(submit(headId, billNo, "0", sourceAccountId, BigDecimal.ONE.negate(),
                            BigDecimal.ONE.negate(), null, List.of(detail(sourceAccountId, BigDecimal.ONE))),
                    ExceptionConstants.ACCOUNT_HEAD_ACCOUNT_REPEAT_CODE);
        } finally {
            cleanup(headId, false);
        }
    }

    @Test
    @DisplayName("审核时重新校验已保存的转账金额")
    void auditRevalidatesPersistedGiro() throws Exception {
        assumeTrue(hasBaseData(), "缺少转账单测试账户");
        String billNo = billNo();
        Long headId = null;
        try {
            assertSuccess(singleTransfer(sourceAccountId, targetAccountId, BigDecimal.ONE,
                    BigDecimal.ONE.negate(), BigDecimal.ONE.negate(), billNo));
            headId = getHeadId(billNo);
            updateStoredAmounts(headId, BigDecimal.ONE, BigDecimal.ONE);
            assertBizCode(setStatus(headId, "1"), ExceptionConstants.ACCOUNT_HEAD_GIRO_AMOUNT_FAILED_CODE);
        } finally {
            if(headId != null) {
                updateStoredAmounts(headId, BigDecimal.ONE.negate(), BigDecimal.ONE.negate());
            }
            cleanup(headId, false);
        }
    }

    private boolean hasBaseData() {
        return jshToken != null && sourceAccountId != null && targetAccountId != null
                && secondTargetAccountId != null;
    }

    private Response singleTransfer(Long sourceId, Long targetId, BigDecimal amount,
                                    BigDecimal totalPrice, BigDecimal changeAmount, String fixedBillNo) {
        return submit(null, fixedBillNo == null ? billNo() : fixedBillNo, "0", sourceId,
                totalPrice, changeAmount, null, List.of(detail(targetId, amount)));
    }

    private Response submit(Long id, String billNo, String status, Long sourceId,
                            BigDecimal totalPrice, BigDecimal changeAmount, Long organId,
                            List<JSONObject> details) {
        JSONObject info = new JSONObject();
        if(id != null) {
            info.put("id", id);
        }
        info.put("type", "转账");
        info.put("billNo", billNo);
        info.put("billTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        info.put("accountId", sourceId);
        info.put("totalPrice", totalPrice);
        info.put("changeAmount", changeAmount);
        info.put("discountMoney", BigDecimal.ZERO);
        info.put("status", status);
        info.put("remark", "转账单自动化测试");
        if(organId != null) {
            info.put("organId", organId);
        }

        JSONArray rows = new JSONArray();
        rows.addAll(details);
        JSONObject body = new JSONObject();
        body.put("info", info.toJSONString());
        body.put("rows", rows.toJSONString());
        RequestSpecification request = tokenReq().body(body.toJSONString());
        return id == null
                ? request.post(CONTEXT + "/accountHead/addAccountHeadAndDetail")
                : request.put(CONTEXT + "/accountHead/updateAccountHeadAndDetail");
    }

    private JSONObject detail(Long accountId, BigDecimal amount) {
        JSONObject detail = new JSONObject();
        detail.put("accountId", accountId);
        detail.put("eachAmount", amount);
        detail.put("remark", "转入账户自动化测试");
        return detail;
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

    private BigDecimal queryNetDelta(Long headId) throws Exception {
        String sql = "select ah.change_amount + coalesce(sum(ai.each_amount),0) "
                + "from jsh_account_head ah left join jsh_account_item ai on ai.header_id=ah.id "
                + "where ah.id=? group by ah.id,ah.change_amount";
        try(Connection connection = dbConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, headId);
            try(ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getBigDecimal(1);
            }
        }
    }

    private void updateStoredAmounts(Long headId, BigDecimal totalPrice, BigDecimal changeAmount) throws Exception {
        try(Connection connection = dbConnection();
            PreparedStatement statement = connection.prepareStatement(
                    "update jsh_account_head set total_price=?,change_amount=? where id=?")) {
            statement.setBigDecimal(1, totalPrice);
            statement.setBigDecimal(2, changeAmount);
            statement.setLong(3, headId);
            assertEquals(1, statement.executeUpdate());
        }
    }

    private void setAccountEnabled(Long accountId, boolean enabled) throws Exception {
        try(Connection connection = dbConnection();
            PreparedStatement statement = connection.prepareStatement(
                    "update jsh_account set enabled=? where id=?")) {
            statement.setBoolean(1, enabled);
            statement.setLong(2, accountId);
            assertEquals(1, statement.executeUpdate());
        }
    }

    private Connection dbConnection() throws Exception {
        return DriverManager.getConnection("jdbc:postgresql://localhost:15432/jsh_erp",
                "postgres", "Postgres@123");
    }

    private String billNo() {
        return "ZZ-T" + System.nanoTime();
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
