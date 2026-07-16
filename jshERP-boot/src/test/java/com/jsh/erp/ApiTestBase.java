package com.jsh.erp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API回归测试基础类 - 提供登录、Token管理、通用断言和测试数据创建
 */
public class ApiTestBase {

    protected static final String BASE_URI = "http://localhost";
    protected static final int PORT = 9999;
    protected static final String CONTEXT = "/jshERP-boot";

    protected static final String ADMIN_LOGIN = "admin";
    protected static final String ADMIN_PASSWORD_MD5 = md5("123456");

    protected static String adminToken;
    protected static Long adminUserId;

    // 自增ID生成器，用于测试数据
    private static final AtomicLong ID_GEN = new AtomicLong(System.currentTimeMillis() % 100000);

    @BeforeAll
    static void setupBase() {
        RestAssured.baseURI = BASE_URI;
        RestAssured.port = PORT;
        // 登录获取token
        if (adminToken == null) {
            loginAsAdmin();
        }
    }

    // ========== 登录 ==========

    protected static void loginAsAdmin() {
        Response resp = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"loginName\":\"" + ADMIN_LOGIN + "\",\"password\":\"" + ADMIN_PASSWORD_MD5 + "\"}")
                .post(CONTEXT + "/user/login");

        assertEquals(200, resp.jsonPath().getInt("code"), "登录接口返回code应为200");
        JSONObject data = JSONObject.parseObject(resp.body().asString()).getJSONObject("data");
        assertNotNull(data, "登录返回data不应为null");
        assertEquals("user can login", data.getString("msgTip"), "登录应成功");

        adminToken = data.getString("token");
        assertNotNull(adminToken, "token不应为null");
        adminUserId = data.getJSONObject("user").getLong("id");
    }

    protected static String login(String loginName, String passwordMd5) {
        Response resp = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"loginName\":\"" + loginName + "\",\"password\":\"" + passwordMd5 + "\"}")
                .post(CONTEXT + "/user/login");
        JSONObject body = JSONObject.parseObject(resp.body().asString());
        if (body.getIntValue("code") == 200) {
            return body.getJSONObject("data").getString("token");
        }
        return null;
    }

    // ========== 请求构建 ==========

    protected RequestSpecification authReq() {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .header("X-Access-Token", adminToken);
    }

    protected RequestSpecification authReqGet() {
        return RestAssured.given()
                .header("X-Access-Token", adminToken);
    }

    protected RequestSpecification noAuthReq() {
        return RestAssured.given().contentType(ContentType.JSON);
    }

    // ========== 通用断言 ==========

    /** 校验标准成功响应 code=200 */
    protected void assertSuccess(Response resp) {
        assertEquals(200, resp.statusCode(), "HTTP状态码应为200");
        String raw = resp.body().asString();
        // 某些接口直接返回JSON数组（如菜单树），此时只要HTTP 200即视为成功
        if (raw.trim().startsWith("[")) {
            return;
        }
        JSONObject body = JSONObject.parseObject(raw);
        assertTrue(body.containsKey("code"), "响应应包含code字段");
        assertEquals(200, body.getIntValue("code"), "业务响应code应为200，实际响应：" + raw);
    }

    /** 校验分页响应 data.total >= 0, data.rows != null */
    protected void assertPaged(Response resp) {
        assertSuccess(resp);
        JSONObject body = JSONObject.parseObject(resp.body().asString());
        JSONObject data = body.getJSONObject("data");
        assertNotNull(data, "data不应为null");
        assertTrue(data.getLong("total") >= 0, "total应>=0");
        assertNotNull(data.getJSONArray("rows"), "rows不应为null");
    }

    /** 校验业务异常响应 code != 200 */
    protected void assertBizError(Response resp) {
        JSONObject body = JSONObject.parseObject(resp.body().asString());
        assertNotEquals(200, body.getIntValue("code"), "业务异常时code不应为200");
    }

    // ========== 工具方法 ==========

    protected static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(input.getBytes());
            return new BigInteger(1, md.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    protected static long nextId() {
        return ID_GEN.incrementAndGet();
    }

    protected String generateNumber(String prefix) {
        Response resp = authReqGet().get(CONTEXT + "/sequence/buildNumber");
        assertSuccess(resp);
        JSONObject body = JSONObject.parseObject(resp.body().asString());
        return body.getJSONObject("data").getString("defaultNumber");
    }

    /** 获取列表第一个记录的ID */
    protected Long getFirstId(Response listResp) {
        JSONArray rows = JSONObject.parseObject(listResp.body().asString())
                .getJSONObject("data").getJSONArray("rows");
        if (rows != null && !rows.isEmpty()) {
            return rows.getJSONObject(0).getLong("id");
        }
        return null;
    }

    // ========== 测试数据创建辅助 ==========

    /** 创建商品，返回materialExtendId */
    protected Long createMaterial(String name, String barCode, Long categoryId, Long unitId) {
        JSONObject material = new JSONObject();
        material.put("name", name);
        material.put("categoryId", categoryId);
        material.put("unitId", unitId);
        material.put("mfrs", "");
        material.put("model", "");
        material.put("standard", "");
        material.put("color", "");
        material.put("brand", "");
        material.put("position", "");
        material.put("remark", "自动化测试创建");
        material.put("enabled", 0);

        Response resp = authReq().body(material.toJSONString()).post(CONTEXT + "/material/add");
        assertSuccess(resp);

        // 使用findBySelect查询刚创建的商品获取ID
        Response listResp = authReqGet()
                .param("q", name)
                .param("page", 1)
                .param("rows", 10)
                .get(CONTEXT + "/material/findBySelect");
        JSONObject data = JSONObject.parseObject(listResp.body().asString());
        JSONArray rows = data.getJSONArray("rows");
        if (rows != null && !rows.isEmpty()) {
            return rows.getJSONObject(0).getLong("id");
        }
        return null;
    }

    /** 创建供应商/客户，返回ID */
    protected Long createSupplier(String name, String type) {
        JSONObject supplier = new JSONObject();
        supplier.put("supplier", name);
        supplier.put("type", type);
        supplier.put("contacts", "测试联系人");
        supplier.put("phonenum", "13800138000");
        supplier.put("telephone", "");
        supplier.put("email", "");
        supplier.put("address", "");
        supplier.put("beginNeedGet", 0);
        supplier.put("beginNeedPay", 0);
        supplier.put("remark", "自动化测试创建");

        Response resp = authReq().body(supplier.toJSONString()).post(CONTEXT + "/supplier/add");
        assertSuccess(resp);

        Response listResp = authReqGet()
                .param("search", "{\"supplier\":\"" + name + "\",\"type\":\"" + type + "\"}")
                .get(CONTEXT + "/supplier/list");
        JSONObject data = JSONObject.parseObject(listResp.body().asString()).getJSONObject("data");
        JSONArray rows = data.getJSONArray("rows");
        if (rows != null && !rows.isEmpty()) {
            return rows.getJSONObject(0).getLong("id");
        }
        return null;
    }

    /** 创建仓库，返回ID */
    protected Long createDepot(String name) {
        JSONObject depot = new JSONObject();
        depot.put("name", name);
        depot.put("type", "1");
        depot.put("remark", "自动化测试创建");

        Response resp = authReq().body(depot.toJSONString()).post(CONTEXT + "/depot/add");
        assertSuccess(resp);

        Response listResp = authReqGet()
                .param("search", "{\"name\":\"" + name + "\"}")
                .get(CONTEXT + "/depot/list");
        JSONObject data = JSONObject.parseObject(listResp.body().asString()).getJSONObject("data");
        JSONArray rows = data.getJSONArray("rows");
        if (rows != null && !rows.isEmpty()) {
            return rows.getJSONObject(0).getLong("id");
        }
        return null;
    }

    /** 创建结算账户，返回ID */
    protected Long createAccount(String name) {
        JSONObject account = new JSONObject();
        account.put("name", name);
        account.put("serialNo", "ACC-" + System.currentTimeMillis());
        account.put("initialAmount", 100000);
        account.put("remark", "自动化测试创建");

        Response resp = authReq().body(account.toJSONString()).post(CONTEXT + "/account/add");
        assertSuccess(resp);

        Response listResp = authReqGet()
                .param("search", "{\"name\":\"" + name + "\"}")
                .get(CONTEXT + "/account/list");
        JSONObject data = JSONObject.parseObject(listResp.body().asString()).getJSONObject("data");
        JSONArray rows = data.getJSONArray("rows");
        if (rows != null && !rows.isEmpty()) {
            return rows.getJSONObject(0).getLong("id");
        }
        return null;
    }

    /** 创建单据（采购入库/销售出库/零售等），返回单据ID */
    protected Long createDepotHeadAndDetail(String type, String subType, Long organId,
                                            Long accountId, Long depotId, Long materialExtendId,
                                            double operNumber, double unitPrice) {
        String number = generateNumber(subType.equals("采购") ? "CGRK" : subType.equals("销售") ? "XSCK" : "LSCK");

        // 查询商品条码
        String barCode = "";
        Response matResp = authReqGet()
                .param("q", "")
                .param("page", 1)
                .param("rows", 100)
                .get(CONTEXT + "/material/findBySelect");
        JSONObject matData = JSONObject.parseObject(matResp.body().asString());
        JSONArray matRows = matData.getJSONArray("rows");
        if (matRows != null) {
            for (int i = 0; i < matRows.size(); i++) {
                if (materialExtendId.equals(matRows.getJSONObject(i).getLong("id"))) {
                    barCode = matRows.getJSONObject(i).getString("mBarCode");
                    break;
                }
            }
        }

        JSONObject info = new JSONObject();
        info.put("number", number);
        info.put("type", type);
        info.put("subType", subType);
        info.put("organId", organId);
        info.put("accountId", accountId);
        info.put("accountIdList", String.valueOf(accountId));
        info.put("accountMoneyList", String.valueOf(operNumber * unitPrice));
        info.put("changeAmount", operNumber * unitPrice);
        info.put("totalPrice", operNumber * unitPrice);
        info.put("deposit", 0);
        info.put("debt", 0);
        info.put("payType", "现付");
        info.put("status", 0);
        info.put("remark", "自动化测试创建");

        // 查询商品单位
        String unit = "";
        if (matRows != null) {
            for (int i = 0; i < matRows.size(); i++) {
                if (materialExtendId.equals(matRows.getJSONObject(i).getLong("id"))) {
                    unit = matRows.getJSONObject(i).getString("unit");
                    break;
                }
            }
        }

        JSONArray rows = new JSONArray();
        JSONObject item = new JSONObject();
        item.put("barCode", barCode);
        item.put("materialExtendId", materialExtendId);
        item.put("depotId", depotId);
        item.put("anotherDepotId", 0);
        item.put("operNumber", operNumber);
        item.put("unitPrice", unitPrice);
        item.put("allPrice", operNumber * unitPrice);
        item.put("taxRate", 0);
        item.put("taxUnitPrice", unitPrice);
        item.put("taxMoney", operNumber * unitPrice);
        item.put("taxLastMoney", operNumber * unitPrice);
        item.put("unit", unit != null ? unit : "");
        item.put("remark", "");
        rows.add(item);

        JSONObject body = new JSONObject();
        body.put("info", info.toJSONString());
        body.put("rows", rows.toJSONString());

        Response resp = authReq().body(body.toJSONString()).post(CONTEXT + "/depotHead/addDepotHeadAndDetail");
        assertSuccess(resp);

        // 查询单据获取ID
        Response listResp = authReqGet()
                .param("search", "{\"number\":\"" + number + "\"}")
                .get(CONTEXT + "/depotHead/list");
        JSONObject data = JSONObject.parseObject(listResp.body().asString()).getJSONObject("data");
        JSONArray listRows = data.getJSONArray("rows");
        if (listRows != null && !listRows.isEmpty()) {
            return listRows.getJSONObject(0).getLong("id");
        }
        return null;
    }

    /** 审核单据 */
    protected void auditDepotHead(Long id) {
        JSONObject body = new JSONObject();
        body.put("status", "1");
        body.put("ids", String.valueOf(id));
        Response resp = authReq().body(body.toJSONString()).post(CONTEXT + "/depotHead/batchSetStatus");
        assertSuccess(resp);
    }

    /** 反审核单据 */
    protected void unauditDepotHead(Long id) {
        JSONObject body = new JSONObject();
        body.put("status", "0");
        body.put("ids", String.valueOf(id));
        Response resp = authReq().body(body.toJSONString()).post(CONTEXT + "/depotHead/batchSetStatus");
        assertSuccess(resp);
    }

    /** 查询商品当前库存 */
    protected double getMaterialStock(Long materialExtendId, Long depotId) {
        Response resp = authReqGet()
                .param("currentPage", 1)
                .param("pageSize", 10)
                .param("depotIds", "")
                .param("categoryId", "")
                .param("position", "")
                .param("materialParam", "")
                .param("zeroStock", 0)
                .param("column", "")
                .param("order", "")
                .get(CONTEXT + "/material/getListWithStock");
        assertSuccess(resp);
        JSONObject data = JSONObject.parseObject(resp.body().asString()).getJSONObject("data");
        if (data != null && data.getJSONArray("rows") != null && !data.getJSONArray("rows").isEmpty()) {
            return data.getJSONArray("rows").getJSONObject(0).getDoubleValue("currentStock");
        }
        return 0;
    }
}
