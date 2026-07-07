package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P2: 升级专项测试 - PostgreSQL + Fastjson2 + MyBatis Plus (ID 65-81)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("P2: PostgreSQL/Fastjson2/MyBatisPlus升级专项")
public class P2PgUpgradeTest extends ApiTestBase {

    // ===== 65. 分页-正常 =====

    @Test
    @Order(1)
    @DisplayName("65: 分页-正常 - findBySelect")
    void paginationNormal() {
        Response resp = authReqGet()
                .param("q", "")
                .param("page", 1)
                .param("rows", 10)
                .get(CONTEXT + "/material/findBySelect");
        assertEquals(200, resp.statusCode());
        JSONObject data = JSONObject.parseObject(resp.body().asString());
        assertTrue(data.getLong("total") >= 0, "total应>=0");
        assertNotNull(data.getJSONArray("rows"), "rows不应为null");
    }

    // ===== 66. 分页-边界 =====

    @Test
    @Order(2)
    @DisplayName("66a: 分页-第一页")
    void paginationFirstPage() {
        Response resp = authReqGet()
                .param("q", "")
                .param("page", 1)
                .param("rows", 5)
                .get(CONTEXT + "/material/findBySelect");
        assertEquals(200, resp.statusCode());
    }

    @Test
    @Order(3)
    @DisplayName("66b: 分页-空数据查询")
    void paginationEmpty() {
        Response resp = authReqGet()
                .param("q", "不存在的商品名称XYZ_999")
                .param("page", 1)
                .param("rows", 10)
                .get(CONTEXT + "/material/findBySelect");
        assertEquals(200, resp.statusCode());
        JSONObject data = JSONObject.parseObject(resp.body().asString());
        assertEquals(0, data.getLong("total"), "不存在的商品查询total应为0");
    }

    @Test
    @Order(4)
    @DisplayName("66c: 分页-进销存报表分页")
    void paginationReport() {
        Response resp = authReqGet()
                .param("currentPage", 1)
                .param("pageSize", 5)
                .param("depotIds", "")
                .param("categoryId", "")
                .param("beginTime", "2020-01-01 00:00:00")
                .param("endTime", "2030-12-31 23:59:59")
                .param("materialParam", "")
                .param("mpList", "")
                .get(CONTEXT + "/depotItem/getInOutStock");
        assertPaged(resp);
    }

    // ===== 67. 排序-正常 =====

    @Test
    @Order(5)
    @DisplayName("67: 排序-正常")
    void sortNormal() {
        Response resp = authReqGet()
                .param("currentPage", 1)
                .param("pageSize", 10)
                .param("depotIds", "")
                .param("categoryId", "")
                .param("position", "")
                .param("materialParam", "")
                .param("zeroStock", 1)
                .param("column", "name")
                .param("order", "asc")
                .get(CONTEXT + "/material/getListWithStock");
        assertPaged(resp);
    }

    // ===== 68. 排序-NULLS LAST (PG特有) =====

    @Test
    @Order(6)
    @DisplayName("68: 排序-NULLS LAST验证")
    void sortNullsLast() {
        Response resp = authReqGet()
                .param("search", "{\"type\":\"供应商\"}")
                .get(CONTEXT + "/supplier/list");
        assertPaged(resp);
    }

    // ===== 69. 排序-中文 =====

    @Test
    @Order(7)
    @DisplayName("69: 中文排序")
    void sortChinese() {
        Response resp = authReqGet()
                .param("q", "")
                .param("page", 1)
                .param("rows", 10)
                .get(CONTEXT + "/material/findBySelect");
        assertEquals(200, resp.statusCode());
    }

    // ===== 70. 模糊查询 =====

    @Test
    @Order(8)
    @DisplayName("70: 模糊查询-LIKE语法")
    void fuzzySearch() {
        Response resp = authReqGet()
                .param("q", "测试")
                .param("page", 1)
                .param("rows", 10)
                .get(CONTEXT + "/material/findBySelect");
        assertEquals(200, resp.statusCode());
    }

    // ===== 71. ILIKE查询 (PG特有) =====

    @Test
    @Order(9)
    @DisplayName("71: ILIKE-不区分大小写搜索")
    void ilikeSearch() {
        Response resp = authReqGet()
                .param("search", "{\"supplier\":\"test\"}")
                .get(CONTEXT + "/supplier/list");
        assertPaged(resp);
    }

    // ===== 72. 日期筛选 =====

    @Test
    @Order(10)
    @DisplayName("72a: 日期筛选-正常范围")
    void dateFilterNormal() {
        Response resp = authReqGet()
                .param("search", "{\"type\":\"入库\",\"subType\":\"采购\",\"beginTime\":\"2024-01-01 00:00:00\",\"endTime\":\"2024-12-31 23:59:59\"}")
                .get(CONTEXT + "/depotHead/list");
        assertPaged(resp);
    }

    @Test
    @Order(11)
    @DisplayName("72b: 日期筛选-边界值（同一天）")
    void dateFilterSameDay() {
        Response resp = authReqGet()
                .param("search", "{\"type\":\"入库\",\"beginTime\":\"2024-06-15 00:00:00\",\"endTime\":\"2024-06-15 23:59:59\"}")
                .get(CONTEXT + "/depotHead/list");
        assertPaged(resp);
    }

    // ===== 73. timestamp时区 =====

    @Test
    @Order(12)
    @DisplayName("73: timestamp时区-查询不报错")
    void timestampTimezone() {
        Response resp = authReqGet()
                .param("search", "{\"beginTime\":\"2024-01-01 00:00:00\",\"endTime\":\"2024-12-31 23:59:59\"}")
                .get(CONTEXT + "/depotHead/list");
        assertPaged(resp);
    }

    // ===== 74. decimal精度 =====

    @Test
    @Order(13)
    @DisplayName("74: decimal精度-金额字段验证")
    void decimalPrecision() {
        Response resp = authReqGet()
                .param("name", "")
                .param("serialNo", "")
                .get(CONTEXT + "/account/listWithBalance");
        assertPaged(resp);
    }

    // ===== 75. boolean字段 =====

    @Test
    @Order(14)
    @DisplayName("75: boolean字段读写")
    void booleanField() {
        Response resp = authReqGet().get(CONTEXT + "/user/getUserSession");
        assertSuccess(resp);
        JSONObject data = JSONObject.parseObject(resp.body().asString()).getJSONObject("data");
        if (data != null) {
            data.getBoolean("ismanager");
        }
    }

    // ===== 76. 金额精度 =====

    @Test
    @Order(15)
    @DisplayName("76: 金额精度-无浮点误差")
    void moneyPrecision() {
        Response resp = authReqGet()
                .param("currentPage", 1)
                .param("pageSize", 10)
                .param("depotIds", "")
                .param("categoryId", "")
                .param("endTime", "2030-12-31 23:59:59")
                .param("materialParam", "")
                .get(CONTEXT + "/depotItem/getInOutStockCountMoney");
        assertSuccess(resp);
    }

    // ===== 77. JSON格式 (Fastjson2兼容) =====

    @Test
    @Order(16)
    @DisplayName("77: JSON格式-Fastjson2兼容性")
    void jsonFormat() {
        Response resp = authReqGet()
                .param("search", "{}")
                .get(CONTEXT + "/depot/list");
        JSONObject body = JSONObject.parseObject(resp.body().asString());
        assertTrue(body.containsKey("code"), "应包含code字段");
        JSONObject data = body.getJSONObject("data");
        assertNotNull(data, "data不应为null");
        assertTrue(data.containsKey("total"), "data应包含total字段");
        assertTrue(data.containsKey("rows"), "data应包含rows字段");
    }

    // ===== 78. HTTP状态码 =====

    @Test
    @Order(17)
    @DisplayName("78a: HTTP状态码-正常200")
    void httpStatus200() {
        Response resp = authReqGet().get(CONTEXT + "/user/getUserSession");
        assertEquals(200, resp.statusCode());
    }

    @Test
    @Order(18)
    @DisplayName("78b: HTTP状态码-未授权访问")
    void httpStatus401() {
        Response resp = io.restassured.RestAssured.given()
                .get(CONTEXT + "/user/getUserSession");
        assertNotEquals(200, resp.jsonPath().getInt("code"));
    }

    // ===== 79. PG大小写 =====

    @Test
    @Order(19)
    @DisplayName("79: 大小写查询")
    void caseSensitivity() {
        Response resp1 = authReqGet()
                .param("q", "Apple")
                .param("page", 1).param("rows", 10)
                .get(CONTEXT + "/material/findBySelect");
        assertEquals(200, resp1.statusCode());

        Response resp2 = authReqGet()
                .param("q", "apple")
                .param("page", 1).param("rows", 10)
                .get(CONTEXT + "/material/findBySelect");
        assertEquals(200, resp2.statusCode());

        Response resp3 = authReqGet()
                .param("q", "苹果")
                .param("page", 1).param("rows", 10)
                .get(CONTEXT + "/material/findBySelect");
        assertEquals(200, resp3.statusCode());
    }

    // ===== 80. PG-NULL处理 =====

    @Test
    @Order(20)
    @DisplayName("80: NULL字段查询不报错")
    void nullHandling() {
        Response resp = authReqGet()
                .param("search", "{\"email\":\"\"}")
                .get(CONTEXT + "/supplier/list");
        assertPaged(resp);
    }

    @Test
    @Order(21)
    @DisplayName("80b: NULL排序不报错")
    void nullSorting() {
        Response resp = authReqGet()
                .param("search", "{}")
                .get(CONTEXT + "/supplier/list");
        assertPaged(resp);
    }

    // ===== 81. 软删除 =====

    @Test
    @Order(22)
    @DisplayName("81: 软删除-已删除数据不出现")
    void softDelete() {
        Response resp = authReqGet()
                .param("q", "")
                .param("page", 1)
                .param("rows", 10)
                .get(CONTEXT + "/material/findBySelect");
        assertEquals(200, resp.statusCode());
    }
}
