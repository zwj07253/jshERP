package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.MaterialPropertyMapper;
import com.jsh.erp.datasource.mappers.MaterialPropertyMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.LogService;
import com.jsh.erp.service.MaterialPropertyService;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialPropertyServiceTest {
    @Mock private MaterialPropertyMapper materialPropertyMapper;
    @Mock private MaterialPropertyMapperEx materialPropertyMapperEx;
    @Mock private UserService userService;
    @Mock private LogService logService;
    @InjectMocks private MaterialPropertyService service;

    @Test
    void rejectsMutationWithoutPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasButtonPermission(101L, "/material/material_property", "1")).thenReturn(false);

        JSONObject body = new JSONObject();
        body.put("nativeName", "扩展1");
        body.put("anotherName", "颜色");
        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> service.insertMaterialProperty(body, null));

        assertEquals(ExceptionConstants.MATERIAL_PERMISSION_CODE, exception.getCode());
        verify(materialPropertyMapper, never()).insertSelective(any());
    }
}
