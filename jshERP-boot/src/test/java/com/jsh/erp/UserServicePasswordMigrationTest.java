package com.jsh.erp;

import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.entities.UserExample;
import com.jsh.erp.datasource.mappers.UserMapper;
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

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServicePasswordMigrationTest {

    @Mock private UserMapper userMapper;
    @Mock private TenantService tenantService;
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
