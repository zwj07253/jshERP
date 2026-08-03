package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.MaterialAttribute;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.MaterialAttributeMapper;
import com.jsh.erp.datasource.mappers.MaterialAttributeMapperEx;
import com.jsh.erp.datasource.mappers.MaterialMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.LogService;
import com.jsh.erp.service.MaterialAttributeService;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialAttributeServiceTest {
    @Mock private MaterialAttributeMapper materialAttributeMapper;
    @Mock private MaterialAttributeMapperEx materialAttributeMapperEx;
    @Mock private MaterialMapperEx materialMapperEx;
    @Mock private UserService userService;
    @Mock private LogService logService;
    @InjectMocks private MaterialAttributeService service;

    @Test
    void rejectsMutationWithoutPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasButtonPermission(101L, "/material/material_attribute", "1")).thenReturn(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> service.insertMaterialAttribute(new JSONObject(), null));

        assertEquals(ExceptionConstants.MATERIAL_PERMISSION_CODE, exception.getCode());
        verify(materialAttributeMapper, never()).insertSelective(any());
    }

    @Test
    void rejectsUpdateWhenTenantMismatch() throws Exception {
        User user = new User();
        user.setId(101L);
        user.setTenantId(1L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasButtonPermission(101L, "/material/material_attribute", "1")).thenReturn(true);

        // DB中的记录属于租户2
        MaterialAttribute existing = new MaterialAttribute();
        existing.setId(1L);
        existing.setAttributeName("颜色");
        existing.setAttributeValue("红|蓝");
        existing.setTenantId(2L);
        existing.setDeleteFlag("0");
        when(materialAttributeMapper.selectByExample(any())).thenReturn(Collections.singletonList(existing));

        JSONObject obj = new JSONObject();
        obj.put("id", 1L);
        obj.put("attributeName", "颜色");
        obj.put("attributeValue", "红|蓝");

        assertThrows(BusinessRunTimeException.class,
                () -> service.updateMaterialAttribute(obj, new MockHttpServletRequest()));
    }

    @Test
    void rejectsDeleteWhenTenantMismatch() throws Exception {
        User user = new User();
        user.setId(101L);
        user.setTenantId(1L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasButtonPermission(101L, "/material/material_attribute", "1")).thenReturn(true);

        // DB中的记录属于租户2
        MaterialAttribute existing = new MaterialAttribute();
        existing.setId(1L);
        existing.setAttributeName("颜色");
        existing.setAttributeValue("红|蓝");
        existing.setTenantId(2L);
        existing.setDeleteFlag("0");
        when(materialAttributeMapper.selectByExample(any())).thenReturn(Collections.singletonList(existing));

        assertThrows(BusinessRunTimeException.class,
                () -> service.deleteMaterialAttribute(1L, new MockHttpServletRequest()));
    }

    @Test
    void rejectsAttributeValueChangeWhenInUse() throws Exception {
        User user = new User();
        user.setId(101L);
        user.setTenantId(1L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasButtonPermission(101L, "/material/material_attribute", "1")).thenReturn(true);

        MaterialAttribute existing = new MaterialAttribute();
        existing.setId(1L);
        existing.setAttributeName("颜色");
        existing.setAttributeValue("红|蓝");
        existing.setTenantId(1L);
        existing.setDeleteFlag("0");
        // getInfoById 返回已有记录，checkIsNameExist 返回空（名称不重复）
        when(materialAttributeMapper.selectByExample(any()))
                .thenReturn(Collections.singletonList(existing))  // getInfoById
                .thenReturn(Collections.emptyList());             // checkIsNameExist
        // 属性已被商品使用
        when(materialMapperEx.getCountByMaterialAttributeIds(any())).thenReturn(1);

        JSONObject obj = new JSONObject();
        obj.put("id", 1L);
        obj.put("attributeName", "颜色");
        obj.put("attributeValue", "红|绿"); // 修改了属性值

        assertThrows(BusinessRunTimeException.class,
                () -> service.updateMaterialAttribute(obj, new MockHttpServletRequest()));
    }
}
