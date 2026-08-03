package com.jsh.erp;

import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.LogMapper;
import com.jsh.erp.datasource.mappers.LogMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.AuditLogWriter;
import com.jsh.erp.service.LogService;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogServiceTest {
    @Mock private LogMapper logMapper;
    @Mock private LogMapperEx logMapperEx;
    @Mock private UserService userService;
    @Mock private AuditLogWriter auditLogWriter;
    @Mock private HttpServletRequest request;

    @InjectMocks private LogService logService;

    @Test
    void rejectsReadWithoutLogMenuPermission() throws Exception {
        User user = user(7L, 10L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(7L, "/system/log")).thenReturn(false);

        assertThrows(BusinessRunTimeException.class, logService::checkReadPermission);
    }

    @Test
    void writesTrustedAndBoundedServerSideAuditData() throws Exception {
        stubRequestUser();
        when(request.getRemoteAddr()).thenReturn("10.0.0.8");

        logService.insertLog("M".repeat(600), "C".repeat(5100), request);

        ArgumentCaptor<String> operation = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> ip = ArgumentCaptor.forClass(String.class);
        verify(auditLogWriter).write(eq(7L), eq(10L), operation.capture(), content.capture(),
                ip.capture(), eq((byte) 0), any(Date.class));
        assertEquals(500, operation.getValue().length());
        assertEquals(5000, content.getValue().length());
        assertEquals("10.0.0.8", ip.getValue());
        verify(request, never()).getHeader(anyString());
    }

    @Test
    void auditStorageFailureDoesNotFailBusinessOperation() throws Exception {
        stubRequestUser();
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        org.mockito.Mockito.doThrow(new RuntimeException("database unavailable"))
                .when(auditLogWriter).write(anyLong(), anyLong(), anyString(), anyString(),
                        anyString(), anyByte(), any(Date.class));

        assertDoesNotThrow(() -> logService.insertLog("用户", "新增用户", request));
    }

    @Test
    void recordsAuthenticatedRequestFailureWithFailureStatus() throws Exception {
        stubRequestUser();
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/account/add");
        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);

        logService.insertFailureLog(new IllegalArgumentException("bad input"), request);

        verify(auditLogWriter).write(eq(7L), eq(10L), eq("请求失败"), content.capture(),
                eq("127.0.0.1"), eq((byte) 1), any(Date.class));
        assertTrue(content.getValue().contains("POST /account/add"));
        assertTrue(content.getValue().contains("IllegalArgumentException"));
    }

    private void stubRequestUser() throws Exception {
        when(userService.getUserId(request)).thenReturn(7L);
        when(userService.getUser(7L)).thenReturn(user(7L, 10L));
    }

    private User user(Long id, Long tenantId) {
        User user = new User();
        user.setId(id);
        user.setTenantId(tenantId);
        return user;
    }
}
