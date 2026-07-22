package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.controller.UserController;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerSecurityTest {

    @Mock private UserService userService;
    @Mock private HttpServletRequest request;
    @InjectMocks private UserController controller;

    @Test
    void infoUsesSanitizedServiceResult() throws Exception {
        User safe = new User();
        safe.setId(8L);
        safe.setLoginName("demo");
        when(userService.getSafeUser(8L)).thenReturn(safe);

        String response = controller.getList(8L, request);

        verify(userService).getSafeUser(8L);
        assertFalse(response.contains("password"));
        assertFalse(response.contains("weixinOpenId"));
    }

    @Test
    void passwordChangeIgnoresClientSuppliedUserId() throws Exception {
        JSONObject body = new JSONObject();
        body.put("userId", 999L);
        body.put("oldpassword", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        body.put("password", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        when(userService.updateCurrentUserPassword(
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", request)).thenReturn(1);

        controller.updatePwd(body, request);

        verify(userService).updateCurrentUserPassword(
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", request);
    }

    @Test
    void listStopsBeforeQueryWhenReadPermissionIsMissing() throws Exception {
        BusinessRunTimeException denied = new BusinessRunTimeException(1, "denied");
        org.mockito.Mockito.doThrow(denied).when(userService).checkReadPermission();

        assertThrows(BusinessRunTimeException.class, () -> controller.getList("{}", request));

        verify(userService, never()).select(any(), any());
    }
}
