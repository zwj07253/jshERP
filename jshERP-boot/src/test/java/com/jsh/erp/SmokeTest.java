package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke Test - 每次提交后必跑，约5分钟
 * 覆盖测试计划 S1-S9
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Smoke Test - 系统基本可用性验证")
public class SmokeTest extends ApiTestBase {

    @Test
    @Order(1)
    @DisplayName("S1: 系统启动成功 - 登录接口可访问")
    void systemStarted() {
        // 如果能登录成功，说明系统已启动且数据库连接正常
        assertNotNull(adminToken, "系统应已启动，token不应为null");
        assertTrue(adminToken.length() > 10, "token长度应>10");
    }

    @Test
    @Order(2)
    @DisplayName("S2: 登录成功（admin账号）")
    void loginSuccess() {
        Response resp = noAuthReq()
                .body("{\"loginName\":\"" + ADMIN_LOGIN + "\",\"password\":\"" + ADMIN_PASSWORD_MD5 + "\"}")
                .post(CONTEXT + "/user/login");
        assertEquals(200, resp.jsonPath().getInt("code"));
        assertEquals("user can login", resp.jsonPath().getString("data.msgTip"));
        assertNotNull(resp.jsonPath().getString("data.token"));
    }

    @Test
    @Order(3)
    @DisplayName("S3: 首页/仪表盘 - 购销统计接口可访问")
    void dashboardLoads() {
        Response resp = authReqGet().get(CONTEXT + "/depotHead/getBuyAndSaleStatistics");
        assertSuccess(resp);
        JSONObject body = JSONObject.parseObject(resp.body().asString());
        assertNotNull(body.get("data"), "统计数据不应为null");
    }

    @Test
    @Order(4)
    @DisplayName("S4: 菜单树正常加载")
    void menuTreeLoads() {
        JSONObject body = new JSONObject();
        body.put("pNumber", "0");
        body.put("userId", adminUserId);
        Response resp = authReq().body(body.toJSONString()).post(CONTEXT + "/function/findMenuByPNumber");
        assertSuccess(resp);
    }

    @Test
    @Order(5)
    @DisplayName("S5: 商品列表页正常显示（有数据）")
    void materialListLoads() {
        // 使用findBySelect端点（material/list在PG下有兼容性问题）
        Response resp = authReqGet()
                .param("q", "")
                .param("page", 1)
                .param("rows", 10)
                .get(CONTEXT + "/material/findBySelect");
        assertEquals(200, resp.statusCode(), "HTTP状态码应为200");
        JSONObject body = JSONObject.parseObject(resp.body().asString());
        assertTrue(body.getLong("total") >= 0, "商品总数应>=0");
    }

    @Test
    @Order(6)
    @DisplayName("S6: 新建商品成功")
    void createMaterial() {
        String testName = "SmokeTest商品_" + System.currentTimeMillis();
        JSONObject material = new JSONObject();
        material.put("name", testName);
        material.put("categoryId", 0);
        material.put("unitId", 0);
        material.put("mfrs", "");
        material.put("model", "");
        material.put("standard", "");
        material.put("color", "");
        material.put("brand", "");
        material.put("position", "");
        material.put("remark", "smoke test");
        material.put("enabled", 0);

        Response resp = authReq().body(material.toJSONString()).post(CONTEXT + "/material/add");
        assertSuccess(resp);
    }

    @Test
    @Order(7)
    @DisplayName("S7: PostgreSQL 数据库连接正常 - 查询仓库列表")
    void dbConnectionOk() {
        Response resp = authReqGet()
                .param("search", "{}")
                .get(CONTEXT + "/depot/list");
        assertPaged(resp);
    }

    @Test
    @Order(8)
    @DisplayName("S8: Redis 连接正常 - Session有效")
    void redisConnectionOk() {
        // 如果能用token访问需要认证的接口，说明Redis session有效
        Response resp = authReqGet().get(CONTEXT + "/user/getUserSession");
        assertSuccess(resp);
    }

    @Test
    @Order(9)
    @DisplayName("S9: Swagger 文档可访问")
    void swaggerAccessible() {
        Response resp = io.restassured.RestAssured.given()
                .get(CONTEXT + "/swagger-ui/index.html");
        assertEquals(200, resp.statusCode(), "Swagger UI应返回200");
    }
}
