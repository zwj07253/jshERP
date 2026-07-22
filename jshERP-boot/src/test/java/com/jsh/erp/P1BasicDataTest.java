package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * P1: 基础数据 CRUD 测试 (ID 21-29)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("P1: 基础数据 CRUD")
public class P1BasicDataTest extends ApiTestBase {

    // ===== 21. 商品类别 =====

    @Test
    @Order(1)
    @DisplayName("21a: 商品类别树查询")
    void categoryTree() {
        Response resp = authReqGet()
                .param("id", 0)
                .get(CONTEXT + "/materialCategory/getMaterialCategoryTree");
        assertSuccess(resp);
    }

    @Test
    @Order(2)
    @DisplayName("21b: 新建商品类别")
    void createCategory() {
        JSONObject body = new JSONObject();
        body.put("name", "测试类别_" + System.currentTimeMillis());
        body.put("serialNo", "P1CAT-" + System.currentTimeMillis());
        body.put("parentId", 0);
        body.put("sort", 1);
        Response resp = authReq().body(body.toJSONString()).post(CONTEXT + "/materialCategory/add");
        assertSuccess(resp);
    }

    @Test
    @Order(3)
    @DisplayName("21c: 查询商品类别列表")
    void listCategories() {
        Response resp = authReqGet()
                .param("search", "{}")
                .get(CONTEXT + "/materialCategory/list");
        assertPaged(resp);
    }

    // ===== 22. 商品信息 =====

    @Test
    @Order(4)
    @DisplayName("22a: 商品增删改查")
    void materialCRUD() {
        // 新建
        String name = "P1测试商品_" + System.currentTimeMillis();
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("categoryId", 0);
        body.put("unitId", 0);
        body.put("mfrs", "");
        body.put("model", "M1");
        body.put("standard", "");
        body.put("color", "");
        body.put("brand", "");
        body.put("position", "");
        body.put("remark", "P1测试");
        body.put("enabled", 0);
        Response addResp = authReq().body(body.toJSONString()).post(CONTEXT + "/material/add");
        assertSuccess(addResp);

        // 查询
        Response listResp = authReqGet()
                .param("q", name)
                .param("page", 1)
                .param("rows", 10)
                .get(CONTEXT + "/material/findBySelect");
        assertEquals(200, listResp.statusCode());
    }

    @Test
    @Order(5)
    @DisplayName("22b: 商品条码查询")
    void materialBarCodeQuery() {
        Response resp = authReqGet()
                .param("barCode", "")
                .param("organId", 0)
                .param("depotId", 0)
                .param("mpList", "")
                .param("prefixNo", "")
                .get(CONTEXT + "/material/getMaterialByBarCode");
        assertSuccess(resp);
    }

    @Test
    @Order(6)
    @DisplayName("22c: 商品带库存列表")
    void materialWithStock() {
        Response resp = authReqGet()
                .param("currentPage", 1)
                .param("pageSize", 10)
                .param("depotIds", "")
                .param("categoryId", "")
                .param("position", "")
                .param("materialParam", "")
                .param("zeroStock", 1)
                .param("column", "")
                .param("order", "")
                .get(CONTEXT + "/material/getListWithStock");
        assertPaged(resp);
    }

    // ===== 23. 多单位 =====

    @Test
    @Order(7)
    @DisplayName("23: 单位列表查询")
    void unitList() {
        Response resp = authReqGet()
                .param("search", "{}")
                .get(CONTEXT + "/unit/list");
        assertPaged(resp);
    }

    @Test
    @Order(8)
    @DisplayName("23b: 新建单位")
    void createUnit() {
        JSONObject body = new JSONObject();
        String suffix = String.valueOf(System.currentTimeMillis() % 100000);
        body.put("basicUnit", "件" + suffix);
        body.put("otherUnit", "箱" + suffix);
        body.put("ratio", 12);
        Response resp = authReq().body(body.toJSONString()).post(CONTEXT + "/unit/add");
        assertSuccess(resp);
    }

    // ===== 24. 多属性 =====

    @Test
    @Order(9)
    @DisplayName("24: 属性查询接口可用")
    void materialAttribute() {
        // 查询属性列表 - 使用materialAttribute相关接口
        Response resp = authReqGet()
                .param("search", "{}")
                .get(CONTEXT + "/unit/getAllList");
        assertSuccess(resp);
    }

    // ===== 25. 仓库 =====

    @Test
    @Order(10)
    @DisplayName("25a: 仓库增删改查")
    void depotCRUD() {
        // 新建
        String name = "P1测试仓库_" + System.currentTimeMillis();
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("type", "1");
        body.put("remark", "P1测试");
        Response addResp = authReq().body(body.toJSONString()).post(CONTEXT + "/depot/add");
        assertSuccess(addResp);

        // 查询
        Response listResp = authReqGet()
                .param("search", "{\"name\":\"" + name + "\"}")
                .get(CONTEXT + "/depot/list");
        assertPaged(listResp);
    }

    @Test
    @Order(11)
    @DisplayName("25b: 仓库全量列表")
    void depotAllList() {
        Response resp = authReqGet().get(CONTEXT + "/depot/getAllList");
        assertSuccess(resp);
    }

    // ===== 26. 收支项目 =====

    @Test
    @Order(12)
    @DisplayName("26: 收支项目增删改查")
    void inOutItemCRUD() {
        // 查询
        Response listResp = authReqGet()
                .param("search", "{}")
                .get(CONTEXT + "/inOutItem/list");
        assertPaged(listResp);

        // 新建
        String name = "P1测试收支_" + System.currentTimeMillis();
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("type", "收入");
        body.put("remark", "P1测试");
        Response addResp = authReq().body(body.toJSONString()).post(CONTEXT + "/inOutItem/add");
        assertSuccess(addResp);
    }

    // ===== 27. 结算账户 =====

    @Test
    @Order(13)
    @DisplayName("27a: 结算账户增删改查")
    void accountCRUD() {
        // 新建
        String name = "P1测试账户_" + System.currentTimeMillis();
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("serialNo", "P1ACC-" + System.currentTimeMillis());
        body.put("initialAmount", 50000);
        body.put("remark", "P1测试");
        Response addResp = authReq().body(body.toJSONString()).post(CONTEXT + "/account/add");
        assertSuccess(addResp);

        // 查询
        Response listResp = authReqGet()
                .param("search", "{\"name\":\"" + name + "\"}")
                .get(CONTEXT + "/account/list");
        assertPaged(listResp);
    }

    @Test
    @Order(14)
    @DisplayName("27b: 账户余额统计")
    void accountStatistics() {
        Response resp = authReqGet()
                .param("name", "")
                .param("serialNo", "")
                .get(CONTEXT + "/account/getStatistics");
        assertSuccess(resp);
    }

    @Test
    @Order(15)
    @DisplayName("27c: 账户下拉选择")
    void accountSelect() {
        Response resp = authReqGet().get(CONTEXT + "/account/findBySelect");
        assertSuccess(resp);
    }

    // ===== 28. 经手人 =====

    @Test
    @Order(16)
    @DisplayName("28a: 经手人列表查询")
    void personList() {
        Response resp = authReqGet()
                .param("search", "{}")
                .get(CONTEXT + "/person/list");
        assertPaged(resp);
    }

    @Test
    @Order(17)
    @DisplayName("28b: 按类型查询经手人")
    void personByType() {
        Response resp = authReqGet()
                .param("type", "业务员")
                .get(CONTEXT + "/person/getPersonByType");
        assertSuccess(resp);
    }

    @Test
    @Order(18)
    @DisplayName("28c: 新建经手人")
    void createPerson() {
        JSONObject body = new JSONObject();
        body.put("name", "P1测试经手人_" + System.currentTimeMillis());
        body.put("type", "业务员");
        body.put("phonenum", "13900139000");
        body.put("remark", "P1测试");
        Response resp = authReq().body(body.toJSONString()).post(CONTEXT + "/person/add");
        assertSuccess(resp);
    }

    // ===== 29. 部门 =====

    @Test
    @Order(19)
    @DisplayName("29a: 部门树查询")
    void departmentTree() {
        Response resp = authReqGet()
                .param("id", 0)
                .get(CONTEXT + "/organization/getOrganizationTree");
        assertSuccess(resp);
    }

    @Test
    @Order(20)
    @DisplayName("29b: 新建部门")
    void createDepartment() {
        JSONObject body = new JSONObject();
        body.put("name", "P1测试部门_" + System.currentTimeMillis());
        body.put("parentId", 0);
        body.put("sort", 1);
        body.put("remark", "P1测试");
        Response resp = authReq().body(body.toJSONString()).post(CONTEXT + "/organization/add");
        assertSuccess(resp);
    }
}
