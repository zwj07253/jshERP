package com.jsh.erp;

import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.datasource.entities.PlatformConfig;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.PlatformConfigMapper;
import com.jsh.erp.datasource.mappers.PlatformConfigMapperEx;
import com.jsh.erp.service.PlatformConfigService;
import com.jsh.erp.service.RedisService;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformConfigServiceTest {

    @Mock private PlatformConfigMapper platformConfigMapper;
    @Mock private PlatformConfigMapperEx platformConfigMapperEx;
    @Mock private UserService userService;
    @Mock private RedisService redisService;
    @Mock private HttpServletRequest request;

    @InjectMocks private PlatformConfigService platformConfigService;

    // --- getInfoByKey: whitelist tests ---

    @Test
    void getInfoByKeyReturnsNullForSensitiveKey() throws Exception {
        assertNull(platformConfigService.getInfoByKey("activation_code"));
        assertNull(platformConfigService.getInfoByKey("email_auth_code"));
        assertNull(platformConfigService.getInfoByKey("aliOss_accessKeySecret"));
        assertNull(platformConfigService.getInfoByKey("weixinSecret"));
        assertNull(platformConfigService.getInfoByKey("email_smtp_host"));
    }

    @Test
    void getInfoByKeyReturnsValueForPublicKey() throws Exception {
        PlatformConfig config = new PlatformConfig();
        config.setPlatformKey("platform_name");
        config.setPlatformValue("TEST_ERP");
        when(platformConfigMapper.selectByExample(any())).thenReturn(Collections.singletonList(config));

        PlatformConfig result = platformConfigService.getInfoByKey("platform_name");

        assertNotNull(result);
        assertEquals("TEST_ERP", result.getPlatformValue());
    }

    @Test
    void getInfoByKeyReturnsNullWhenKeyNotInWhitelist() throws Exception {
        assertNull(platformConfigService.getInfoByKey("some_random_key"));
        verify(platformConfigMapper, never()).selectByExample(any());
    }

    // --- updatePlatformConfigByKey: key validation tests ---

    @Test
    void updateRejectsNonMutableKey() throws Exception {
        User admin = new User();
        admin.setLoginName(BusinessConstants.DEFAULT_MANAGER);
        when(userService.getCurrentUser()).thenReturn(admin);

        int result = platformConfigService.updatePlatformConfigByKey("activation_code", "newValue");

        assertEquals(0, result);
        verify(platformConfigMapper, never()).updateByExampleSelective(any(), any());
    }

    @Test
    void updateAllowsMutableKey() throws Exception {
        User admin = new User();
        admin.setLoginName(BusinessConstants.DEFAULT_MANAGER);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(platformConfigMapper.updateByExampleSelective(any(), any())).thenReturn(1);

        int result = platformConfigService.updatePlatformConfigByKey("platform_name", "NewName");

        assertEquals(1, result);
        verify(platformConfigMapper).updateByExampleSelective(any(), any());
    }

    @Test
    void updateAllowsSensitiveMutableKeys() throws Exception {
        User admin = new User();
        admin.setLoginName(BusinessConstants.DEFAULT_MANAGER);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(platformConfigMapper.updateByExampleSelective(any(), any())).thenReturn(1);

        // These are sensitive but in MUTABLE_PLATFORM_KEYS (admin can edit them via modal)
        assertEquals(1, platformConfigService.updatePlatformConfigByKey("aliOss_accessKeySecret", "newSecret"));
        assertEquals(1, platformConfigService.updatePlatformConfigByKey("weixinSecret", "newSecret"));
        assertEquals(1, platformConfigService.updatePlatformConfigByKey("email_auth_code", "newCode"));
    }

    @Test
    void updateRejectsNonAdminUser() throws Exception {
        User normalUser = new User();
        normalUser.setLoginName("tenant_user");
        when(userService.getCurrentUser()).thenReturn(normalUser);

        int result = platformConfigService.updatePlatformConfigByKey("platform_name", "NewName");

        assertEquals(0, result);
        verify(platformConfigMapper, never()).updateByExampleSelective(any(), any());
    }
}
