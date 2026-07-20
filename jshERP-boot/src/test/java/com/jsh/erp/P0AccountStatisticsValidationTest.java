package com.jsh.erp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("P0: 账户统计金额与流水余额")
public class P0AccountStatisticsValidationTest extends ApiTestBase {

    private static final Long MEMBER_ID = 109L;

    @Test
    @DisplayName("收预付款不重复、同秒流水余额稳定、日期包含全天且期初金额不可篡改")
    void accountStatisticsAndFlowAreAccurate() {
        assumeTrue(memberExists(), "缺少会员测试数据，请先导入 watch_test_data.sql");
        String accountName = "账户统计自动化-" + System.currentTimeMillis();
        Long accountId = createAccount(accountName);
        assertNotNull(accountId);

        String billTime = LocalDateTime.now().withNano(0)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String firstBillNo = generateNumber("SYF");
        String secondBillNo = generateNumber("SYF");
        List<Long> headIds = new ArrayList<>();
        try {
            assertSuccess(submitAdvance(firstBillNo, billTime, accountId, new BigDecimal("2.00")));
            headIds.add(getAccountHeadId(firstBillNo));
            assertSuccess(submitAdvance(secondBillNo, billTime, accountId, new BigDecimal("3.00")));
            headIds.add(getAccountHeadId(secondBillNo));

            Response statisticsResponse = authReqGet()
                    .param("name", accountName).param("serialNo", "")
                    .get(CONTEXT + "/account/getStatistics");
            assertSuccess(statisticsResponse);
            JSONObject statistics = JSONObject.parseObject(statisticsResponse.body().asString())
                    .getJSONObject("data");
            assertMoney(new BigDecimal("100005.00"), statistics.getBigDecimal("allCurrentAmount"),
                    "收预付款只能按明细账户统计一次");

            String day = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            Response flowResponse = authReqGet()
                    .param("currentPage", 1).param("pageSize", 10)
                    .param("accountId", accountId)
                    .param("initialAmount", "999999999")
                    .param("number", "")
                    .param("beginTime", day).param("endTime", day)
                    .get(CONTEXT + "/account/findAccountInOutList");
            assertPaged(flowResponse);
            JSONObject flowData = JSONObject.parseObject(flowResponse.body().asString()).getJSONObject("data");
            assertEquals(2, flowData.getIntValue("total"), "结束日期必须包含当天全部流水");
            JSONArray rows = flowData.getJSONArray("rows");
            assertEquals(2, rows.size());
            assertFalse(rows.getJSONObject(0).getString("rowKey").isBlank(), "流水必须返回稳定唯一键");
            assertMoney(new BigDecimal("100005.00"), rows.getJSONObject(0).getBigDecimal("balance"),
                    "客户端伪造的期初金额不能影响余额");
            assertMoney(new BigDecimal("100002.00"), rows.getJSONObject(1).getBigDecimal("balance"),
                    "同一秒发生的流水必须按稳定顺序显示中间余额");

            Response filteredResponse = authReqGet()
                    .param("currentPage", 1).param("pageSize", 10)
                    .param("accountId", accountId).param("number", secondBillNo)
                    .param("beginTime", day).param("endTime", day)
                    .get(CONTEXT + "/account/findAccountInOutList");
            assertPaged(filteredResponse);
            JSONObject filteredData = JSONObject.parseObject(filteredResponse.body().asString()).getJSONObject("data");
            assertEquals(1, filteredData.getIntValue("total"));
            assertMoney(new BigDecimal("100005.00"), filteredData.getJSONArray("rows")
                            .getJSONObject(0).getBigDecimal("balance"),
                    "按单号筛选时余额仍应包含此前全部流水");
        } finally {
            for(int index = headIds.size() - 1; index >= 0; index--) {
                cleanupAccountHead(headIds.get(index));
            }
            authReq().param("id", accountId).delete(CONTEXT + "/account/delete");
        }
    }

    private Response submitAdvance(String billNo, String billTime, Long accountId, BigDecimal amount) {
        JSONObject info = new JSONObject();
        info.put("type", "收预付款");
        info.put("organId", MEMBER_ID);
        info.put("billNo", billNo);
        info.put("billTime", billTime);
        info.put("totalPrice", amount);
        info.put("changeAmount", amount);
        info.put("discountMoney", BigDecimal.ZERO);
        info.put("status", "1");
        info.put("remark", "账户统计自动化测试");

        JSONObject detail = new JSONObject();
        detail.put("accountId", accountId);
        detail.put("eachAmount", amount);
        detail.put("remark", "账户统计自动化测试");
        JSONArray rows = new JSONArray();
        rows.add(detail);

        JSONObject body = new JSONObject();
        body.put("info", info.toJSONString());
        body.put("rows", rows.toJSONString());
        return authReq().body(body.toJSONString()).post(CONTEXT + "/accountHead/addAccountHeadAndDetail");
    }

    private Long getAccountHeadId(String billNo) {
        Response response = authReqGet().param("billNo", billNo)
                .get(CONTEXT + "/accountHead/getDetailByNumber");
        assertSuccess(response);
        return JSONObject.parseObject(response.body().asString()).getJSONObject("data").getLong("id");
    }

    private void cleanupAccountHead(Long headId) {
        if(headId == null) {
            return;
        }
        JSONObject status = new JSONObject();
        status.put("status", "0");
        status.put("ids", String.valueOf(headId));
        assertSuccess(authReq().body(status.toJSONString()).post(CONTEXT + "/accountHead/batchSetStatus"));
        assertSuccess(authReq().param("id", headId).delete(CONTEXT + "/accountHead/delete"));
    }

    private boolean memberExists() {
        Response response = authReqGet().param("id", MEMBER_ID).get(CONTEXT + "/supplier/info");
        JSONObject body = JSONObject.parseObject(response.body().asString());
        return body.getIntValue("code") == 200 && body.getJSONObject("data") != null
                && body.getJSONObject("data").getJSONObject("info") != null;
    }

    private void assertMoney(BigDecimal expected, BigDecimal actual, String message) {
        assertNotNull(actual, message);
        assertEquals(0, expected.compareTo(actual), message + "，expected=" + expected + ", actual=" + actual);
    }
}
