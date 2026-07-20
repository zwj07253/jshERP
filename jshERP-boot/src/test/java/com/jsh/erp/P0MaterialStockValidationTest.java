package com.jsh.erp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P0: 商品库存权限与期初库存校验")
class P0MaterialStockValidationTest extends ApiTestBase {

    private static Long depotId;

    @BeforeAll
    static void loadDepot() {
        P0MaterialStockValidationTest test = new P0MaterialStockValidationTest();
        Response response = test.authReqGet().param("search", "{}").get(CONTEXT + "/depot/list");
        JSONArray rows = JSONObject.parseObject(response.body().asString())
                .getJSONObject("data").getJSONArray("rows");
        if (rows != null && !rows.isEmpty()) {
            depotId = rows.getJSONObject(0).getLong("id");
        }
    }

    @Test
    @DisplayName("无仓库权限用户不能从库存列表或直接库存接口读取数据")
    void warehousePermissionIsFailClosed() {
        String token = login("zhangwei", md5("123456"));
        assertNotNull(token, "测试账号 zhangwei 应可登录");

        Response list = RestAssured.given().header("X-Access-Token", token)
                .param("currentPage", 1).param("pageSize", 10)
                .param("materialParam", "").param("zeroStock", 1)
                .get(CONTEXT + "/material/getListWithStock");
        JSONObject listBody = JSONObject.parseObject(list.body().asString());
        assertEquals(200, listBody.getIntValue("code"));
        assertEquals(0, listBody.getJSONObject("data").getIntValue("total"));

        if (depotId != null) {
            Response direct = RestAssured.given().header("X-Access-Token", token)
                    .param("depotId", depotId).param("barCode", "6901234560017")
                    .get(CONTEXT + "/depotItem/findStockByDepotAndBarCode");
            assertNotEquals(200, JSONObject.parseObject(direct.body().asString()).getIntValue("code"));
        }
    }

    @Test
    @DisplayName("后端拒绝负数期初库存")
    void rejectNegativeInitialStock() {
        if (depotId == null) {
            return;
        }
        JSONObject stock = new JSONObject();
        stock.put("id", depotId);
        stock.put("initStock", -1);
        JSONArray stocks = new JSONArray();
        stocks.add(stock);

        JSONObject material = new JSONObject();
        material.put("name", "非法期初库存-" + nextId());
        material.put("enableSerialNumber", "0");
        material.put("enableBatchNumber", "0");
        material.put("manySku", new JSONArray());
        material.put("stock", stocks);

        Response response = authReq().contentType(ContentType.JSON).body(material.toJSONString())
                .post(CONTEXT + "/material/add");
        JSONObject body = JSONObject.parseObject(response.body().asString());
        assertEquals(ExceptionConstants.MATERIAL_INITIAL_STOCK_INVALID_CODE, body.getIntValue("code"));
    }
}
