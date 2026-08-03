package com.jsh.erp;

import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.entities.UserExample;
import com.jsh.erp.datasource.mappers.UserMapper;
import com.jsh.erp.service.LogService;
import com.jsh.erp.service.RedisService;
import com.jsh.erp.service.TenantService;
import com.jsh.erp.service.UserPasswordService;
import com.jsh.erp.service.UserService;
import com.jsh.erp.utils.ExceptionCodeConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServicePasswordMigrationTest {

    @Mock private UserMapper userMapper;
    @Mock private TenantService tenantService;
    @Mock private RedisService redisService;
    @Mock private LogService logService;
    @Mock private HttpServletRequest request;
    @Spy private UserPasswordService userPasswordService;
    @InjectMocks private UserService userService;

    @Test
    void successfulLegacyLoginUpgradesStoredPassword() throws Exception {
        String legacyCredential = "e10adc3949ba59abbe56e057f20f883e";
        User user = activeUser(legacyCredential);
        when(userMapper.selectByExample(any(UserExample.class))).thenReturn(Collections.singletonList(user));

        int result = userService.validateUser("demo", legacyCredential);

        assertEquals(ExceptionCodeConstants.UserExceptionCode.USER_CONDITION_FIT, result);
        ArgumentCaptor<User> update = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateByPrimaryKeySelective(update.capture());
        assertTrue(update.getValue().getPassword().startsWith("$2"));
        assertTrue(userPasswordService.matches(legacyCredential, update.getValue().getPassword()));
    }

    @Test
    void passwordLoginDoesNotLoadTenantLifecycle() throws Exception {
        String credential = "e10adc3949ba59abbe56e057f20f883e";
        User user = activeUser(credential);
        when(userMapper.selectByExample(any(UserExample.class))).thenReturn(Collections.singletonList(user));

        int result = userService.validateUser("demo", credential);

        assertEquals(ExceptionCodeConstants.UserExceptionCode.USER_CONDITION_FIT, result);
        verifyNoInteractions(tenantService);
    }

    @Test
    void weixinLoginDoesNotLoadTenantLifecycle() throws Exception {
        User user = activeUser("unused");
        user.setWeixinOpenId("openid");

        Map<String, Object> result = userService.loginByWeixin(user, request);

        assertEquals("user can login", result.get("msgTip"));
        verifyNoInteractions(tenantService);
    }

    private User activeUser(String password) {
        User user = new User();
        user.setId(9L);
        user.setLoginName("demo");
        user.setPassword(password);
        user.setStatus((byte) 0);
        user.setTenantId(100L);
        user.setDeleteFlag("0");
        return user;
    }
}
