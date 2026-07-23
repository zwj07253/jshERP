package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.datasource.entities.SystemConfig;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.SystemConfigMapper;
import com.jsh.erp.datasource.mappers.SystemConfigMapperEx;
import com.jsh.erp.service.LogService;
import com.jsh.erp.service.SystemConfigService;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    @Mock private SystemConfigMapper systemConfigMapper;
    @Mock private SystemConfigMapperEx systemConfigMapperEx;
    @Mock private UserService userService;
    @Mock private LogService logService;
    @Mock private HttpServletRequest request;

    @InjectMocks private SystemConfigService systemConfigService;

    @Test
    void insertForcesServerSideTenantId() throws Exception {
        when(userService.getCurrentUser()).thenReturn(user(5L, 42L));
        JSONObject obj = new JSONObject();
        obj.put("id", 999L);
        obj.put("tenantId", 0L);
        obj.put("companyName", "test");

        systemConfigService.insertSystemConfig(obj, request);

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigMapper).insertSelective(captor.capture());
        SystemConfig saved = captor.getValue();
        assertEquals(42L, saved.getTenantId());
        assertNull(saved.getId());
    }

    @Test
    void updateForcesServerSideTenantId() throws Exception {
        when(userService.getCurrentUser()).thenReturn(user(5L, 42L));
        when(systemConfigMapper.updateByPrimaryKeySelective(any(SystemConfig.class))).thenReturn(1);
        JSONObject obj = new JSONObject();
        obj.put("id", 10L);
        obj.put("tenantId", 0L);
        obj.put("companyName", "test");

        systemConfigService.updateSystemConfig(obj, request);

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigMapper).updateByPrimaryKeySelective(captor.capture());
        assertEquals(42L, captor.getValue().getTenantId());
    }

    @Test
    void updateKeepsOriginalId() throws Exception {
        when(userService.getCurrentUser()).thenReturn(user(5L, 42L));
        when(systemConfigMapper.updateByPrimaryKeySelective(any(SystemConfig.class))).thenReturn(1);
        JSONObject obj = new JSONObject();
        obj.put("id", 10L);
        obj.put("companyName", "test");

        systemConfigService.updateSystemConfig(obj, request);

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigMapper).updateByPrimaryKeySelective(captor.capture());
        assertEquals(10L, captor.getValue().getId());
    }

    @Test
    void batchDeletePassesTenantIdToMapper() throws Exception {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            when(userService.getCurrentUser()).thenReturn(user(5L, 42L));

            systemConfigService.batchDeleteSystemConfigByIds("10,20");

            ArgumentCaptor<String[]> idsCaptor = ArgumentCaptor.forClass(String[].class);
            ArgumentCaptor<Long> tenantCaptor = ArgumentCaptor.forClass(Long.class);
            verify(systemConfigMapperEx).batchDeleteSystemConfigByIds(
                    any(), any(), idsCaptor.capture(), tenantCaptor.capture());
            assertEquals(42L, tenantCaptor.getValue());
            assertEquals(2, idsCaptor.getValue().length);
            assertEquals("10", idsCaptor.getValue()[0]);
            assertEquals("20", idsCaptor.getValue()[1]);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    private User user(Long id, Long tenantId) {
        User user = new User();
        user.setId(id);
        user.setTenantId(tenantId);
        return user;
    }
}
