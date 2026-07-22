package com.jsh.erp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.datasource.mappers.MaterialExtendMapperEx;
import com.jsh.erp.datasource.mappers.MaterialExtendMapper;
import com.jsh.erp.datasource.mappers.DepotItemMapperEx;
import com.jsh.erp.datasource.vo.MaterialExtendVo4List;
import com.jsh.erp.service.UserService;
import com.jsh.erp.service.RedisService;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.MaterialExtendService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialExtendServiceTest {

    @Mock
    private MaterialExtendMapperEx materialExtendMapperEx;
    @Mock
    private MaterialExtendMapper materialExtendMapper;
    @Mock
    private DepotItemMapperEx depotItemMapperEx;
    @Mock
    private UserService userService;
    @Mock
    private RedisService redisService;

    @InjectMocks
    private MaterialExtendService materialExtendService;

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void rejectsDetailIdOwnedByAnotherMaterial() throws Exception {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        when(materialExtendMapperEx.getDetailList(10L)).thenReturn(Collections.emptyList());
        JSONObject detail = new JSONObject();
        detail.put("id", 20L);
        detail.put("barCode", "CODE-20");
        JSONArray details = new JSONArray();
        details.add(detail);
        JSONObject body = new JSONObject();
        body.put("meList", details);
        body.put("meDeleteIdList", new JSONArray());

        assertThrows(BusinessRunTimeException.class,
                () -> materialExtendService.saveDetials(body, "[]", 10L, "update"));
    }

    @Test
    void rejectsDuplicateSkuAndBarcode() throws Exception {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        JSONObject first = detail(null, "SKU-A", "CODE-A");
        JSONObject second = detail(null, "SKU-A", "CODE-B");
        JSONObject body = body(first, second);
        body.put("manySku", new JSONArray(Collections.singletonList("颜色")));
        body.put("skuOne", new JSONArray(Collections.singletonList("红")));

        assertThrows(BusinessRunTimeException.class,
                () -> materialExtendService.saveDetials(body, "[]", 10L, "insert"));
    }

    @Test
    void rejectsUpdatingSkuUsedByDocument() throws Exception {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        org.mockito.Mockito.lenient().when(materialExtendMapperEx.getDetailList(10L)).thenReturn(Collections.singletonList(existing(20L, "SKU-A", "CODE-A")));
        when(depotItemMapperEx.getCountByMaterialExtendIds(Collections.singletonList(20L))).thenReturn(1L);
        JSONObject body = body(detail(20L, "SKU-B", "CODE-A"));

        assertThrows(BusinessRunTimeException.class,
                () -> materialExtendService.saveDetials(body, "[]", 10L, "update"));
    }

    @Test
    void treatsMissingIdAsNewDetail() throws Exception {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        when(userService.getCurrentUser()).thenReturn(new com.jsh.erp.datasource.entities.User());
        JSONObject body = body(detail(null, "红", "CODE-A"));
        body.put("manySku", new JSONArray(Collections.singletonList("颜色")));
        body.put("skuOne", new JSONArray(Collections.singletonList("红")));
        when(materialExtendMapperEx.getDetailList(10L)).thenReturn(Collections.emptyList());

        materialExtendService.saveDetials(body, "[]", 10L, "update");
    }

    private JSONObject detail(Long id, String sku, String barCode) {
        JSONObject detail = new JSONObject();
        if (id != null) detail.put("id", id);
        detail.put("sku", sku);
        detail.put("barCode", barCode);
        detail.put("commodityUnit", "件");
        return detail;
    }

    private JSONObject body(JSONObject... rows) {
        JSONObject body = new JSONObject();
        JSONArray details = new JSONArray();
        for (JSONObject row : rows) details.add(row);
        body.put("meList", details);
        body.put("meDeleteIdList", new JSONArray());
        return body;
    }

    private MaterialExtendVo4List existing(Long id, String sku, String barCode) {
        MaterialExtendVo4List detail = new MaterialExtendVo4List();
        detail.setId(id);
        detail.setSku(sku);
        detail.setBarCode(barCode);
        return detail;
    }
}
