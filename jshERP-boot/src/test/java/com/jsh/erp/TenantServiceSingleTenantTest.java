package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.TenantMapper;
import com.jsh.erp.service.TenantService;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceSingleTenantTest {

    @Mock private TenantMapper tenantMapper;
    @Mock private UserService userService;
    @Mock private HttpServletRequest request;
    @InjectMocks private TenantService tenantService;

    @Test
    void platformAdminCanLowerConfiguredLimitWithoutAUsageCheck() throws Exception {
        User admin = new User();
        admin.setLoginName("admin");
        JSONObject input = new JSONObject();
        input.put("tenantId", 1L);
        input.put("userNumLimit", 1);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(tenantMapper.updateByPrimaryKeySelective(any())).thenReturn(1);

        int result = tenantService.updateTenant(input, request);

        assertEquals(1, result);
        verify(tenantMapper).updateByPrimaryKeySelective(any());
    }
}
