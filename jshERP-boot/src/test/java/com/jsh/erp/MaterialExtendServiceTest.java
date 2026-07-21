package com.jsh.erp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.datasource.mappers.MaterialExtendMapperEx;
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
}
