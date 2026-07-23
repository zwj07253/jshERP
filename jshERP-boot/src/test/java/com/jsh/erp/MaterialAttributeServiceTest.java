package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
}
