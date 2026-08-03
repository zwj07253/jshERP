package com.jsh.erp;

import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.UserMapper;
import com.jsh.erp.datasource.mappers.TenantMapper;
import com.jsh.erp.filter.LogCostFilter;
import com.jsh.erp.service.RedisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogCostFilterSecurityTest {

    @Mock private RedisService redisService;
    @Mock private UserMapper userMapper;
    @Mock private TenantMapper tenantMapper;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;
    @InjectMocks private LogCostFilter filter;

    @Test
    void disabledUserSessionIsRejectedImmediately() throws Exception {
        User disabled = new User();
        disabled.setId(9L);
        disabled.setStatus((byte) 2);
        disabled.setDeleteFlag("0");
        when(request.getRequestURI()).thenReturn("/jshERP-boot/user/getUserSession");
        when(redisService.getObjectFromSessionByKey(request, "userId")).thenReturn("9");
        when(userMapper.selectByPrimaryKey(9L)).thenReturn(disabled);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilter(request, response, chain);

        verify(redisService).deleteObjectBySession(request, "userId");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void activeUserSessionContinues() throws Exception {
        User active = new User();
        active.setId(9L);
        active.setStatus((byte) 0);
        active.setDeleteFlag("0");
        when(request.getRequestURI()).thenReturn("/jshERP-boot/user/getUserSession");
        when(redisService.getObjectFromSessionByKey(request, "userId")).thenReturn("9");
        when(userMapper.selectByPrimaryKey(9L)).thenReturn(active);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(redisService, never()).deleteObjectBySession(request, "userId");
    }

    @Test
    void platformAdminDoesNotRequireTenantRecord() throws Exception {
        User admin = new User();
        admin.setId(120L);
        admin.setLoginName("admin");
        admin.setTenantId(0L);
        admin.setStatus((byte) 0);
        admin.setDeleteFlag("0");
        when(request.getRequestURI()).thenReturn("/jshERP-boot/function/findMenuByPNumber");
        when(request.getHeader("X-Access-Token")).thenReturn("token_0");
        when(redisService.getObjectFromSessionByKey(request, "userId")).thenReturn("120");
        when(userMapper.selectByPrimaryKey(120L)).thenReturn(admin);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(tenantMapper);
        verify(redisService, never()).deleteObjectBySession(request, "userId");
    }
}
