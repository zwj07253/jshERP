package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P0: 登录与权限测试 (ID 1-5)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("P0: 登录与权限")
public class P0LoginAuthTest extends ApiTestBase {

    // ===== 1. 登录/登出 =====

    @Test
    @Order(1)
    @DisplayName("1a: 正常登录成功")
    void loginNormal() {
        Response resp = noAuthReq()
                .body("{\"loginName\":\"" + ADMIN_LOGIN + "\",\"password\":\"" + ADMIN_PASSWORD_MD5 + "\"}")
                .post(CONTEXT + "/user/login");
        assertEquals(200, resp.jsonPath().getInt("code"));
        assertEquals("user can login", resp.jsonPath().getString("data.msgTip"));
        assertNotNull(resp.jsonPath().getString("data.token"));
    }

    @Test
    @Order(2)
    @DisplayName("1b: 密码错误登录失败")
    void loginWrongPassword() {
        Response resp = noAuthReq()
                .body("{\"loginName\":\"" + ADMIN_LOGIN + "\",\"password\":\"" + md5("wrongpassword") + "\"}")
                .post(CONTEXT + "/user/login");
        assertEquals(200, resp.jsonPath().getInt("code"));
        assertEquals("user password error", resp.jsonPath().getString("data.msgTip"));
    }

    @Test
    @Order(3)
    @DisplayName("1c: 不存在的用户登录失败")
    void loginNonexistentUser() {
        Response resp = noAuthReq()
                .body("{\"loginName\":\"nonexistent_user_xyz\",\"password\":\"" + md5("123456") + "\"}")
                .post(CONTEXT + "/user/login");
        assertEquals(200, resp.jsonPath().getInt("code"));
        assertEquals("user is not exist", resp.jsonPath().getString("data.msgTip"));
    }

    @Test
    @Order(4)
    @DisplayName("1d: 无Token访问被拒")
    void accessWithoutToken() {
        Response resp = io.restassured.RestAssured.given()
                .get(CONTEXT + "/user/getUserSession");
        // 无token应被LogCostFilter拦截，返回"loginOut"或非200状态码
        String body = resp.body().asString();
        assertTrue(body.contains("loginOut") || resp.statusCode() != 200,
                "无token应被拦截，实际返回: " + body);
    }

    @Test
    @Order(5)
    @DisplayName("1e: 登出后Token失效")
    void logoutTokenInvalid() {
        // 先登录获取新token
        String tempToken = login(ADMIN_LOGIN, ADMIN_PASSWORD_MD5);
        assertNotNull(tempToken, "临时登录应成功");

        // 登出
        Response logoutResp = io.restassured.RestAssured.given()
                .header("X-Access-Token", tempToken)
                .get(CONTEXT + "/user/logout");
        assertSuccess(logoutResp);

        // 用已登出的token访问
        Response accessResp = io.restassured.RestAssured.given()
                .header("X-Access-Token", tempToken)
                .get(CONTEXT + "/user/getUserSession");
        // 登出后token应失效
        String body = accessResp.body().asString();
        assertTrue(body.contains("loginOut") || accessResp.statusCode() != 200,
                "登出后token应失效");
    }

    // ===== 2. 菜单权限 =====

    @Test
    @Order(6)
    @DisplayName("2: 菜单权限 - admin可获取菜单树")
    void menuPermission() {
        JSONObject body = new JSONObject();
        body.put("pNumber", "0");
        body.put("userId", adminUserId);
        Response resp = authReq().body(body.toJSONString()).post(CONTEXT + "/function/findMenuByPNumber");
        assertSuccess(resp);
    }

    // ===== 3. 按钮权限 =====

    @Test
    @Order(7)
    @DisplayName("3: 按钮权限 - 获取当前用户按钮权限")
    void buttonPermission() {
        Response resp = authReqGet().get(CONTEXT + "/user/getUserBtnByCurrentUser");
        assertSuccess(resp);
    }

    // ===== 4. 用户/角色/权限绑定 =====

    @Test
    @Order(8)
    @DisplayName("4: 角色列表可查询")
    void roleBinding() {
        Response resp = authReqGet()
                .param("search", "{}")
                .get(CONTEXT + "/role/list");
        assertPaged(resp);
    }

    // ===== 5. 多租户数据隔离 =====

    @Test
    @Order(9)
    @DisplayName("5: 多租户数据隔离 - admin用户tenantId=0")
    void tenantIsolation() {
        Response resp = authReqGet().get(CONTEXT + "/user/infoWithTenant");
        assertSuccess(resp);
        JSONObject body = JSONObject.parseObject(resp.body().asString());
        JSONObject data = body.getJSONObject("data");
        if (data != null) {
            // admin属于租户0（超级管理员）
            assertEquals(0, data.getIntValue("tenantId"), "admin应属于租户0");
        }
    }
}
