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

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.exception.BusinessRunTimeException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        admin.setTenantId(null);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(userService.isPlatformSuperAdmin(admin)).thenReturn(true);

        int result = platformConfigService.updatePlatformConfigByKey("activation_code", "newValue");

        assertEquals(0, result);
        verify(platformConfigMapper, never()).updateByExampleSelective(any(), any());
    }

    @Test
    void updateAllowsMutableKey() throws Exception {
        User admin = new User();
        admin.setLoginName(BusinessConstants.DEFAULT_MANAGER);
        admin.setTenantId(null);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(userService.isPlatformSuperAdmin(admin)).thenReturn(true);
        when(platformConfigMapper.updateByExampleSelective(any(), any())).thenReturn(1);

        int result = platformConfigService.updatePlatformConfigByKey("platform_name", "NewName");

        assertEquals(1, result);
        verify(platformConfigMapper).updateByExampleSelective(any(), any());
    }

    @Test
    void updateAllowsSensitiveMutableKeys() throws Exception {
        User admin = new User();
        admin.setLoginName(BusinessConstants.DEFAULT_MANAGER);
        admin.setTenantId(null);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(userService.isPlatformSuperAdmin(admin)).thenReturn(true);
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
        normalUser.setTenantId(100L);
        when(userService.getCurrentUser()).thenReturn(normalUser);
        when(userService.isPlatformSuperAdmin(normalUser)).thenReturn(false);

        int result = platformConfigService.updatePlatformConfigByKey("platform_name", "NewName");

        assertEquals(0, result);
        verify(platformConfigMapper, never()).updateByExampleSelective(any(), any());
    }

    // --- Sensitive key masking tests ---

    @Test
    void getPlatformConfigByIdMasksSensitiveKey() throws Exception {
        User admin = new User();
        admin.setLoginName(BusinessConstants.DEFAULT_MANAGER);
        admin.setTenantId(null);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(userService.isPlatformSuperAdmin(admin)).thenReturn(true);

        PlatformConfig config = new PlatformConfig();
        config.setId(1L);
        config.setPlatformKey("aliOss_accessKeySecret");
        config.setPlatformValue("real_secret_123");
        when(platformConfigMapper.selectByPrimaryKey(1L)).thenReturn(config);

        PlatformConfig result = platformConfigService.getPlatformConfig(1L);

        assertNotNull(result);
        assertEquals("******", result.getPlatformValue());
    }

    @Test
    void getPlatformConfigByIdDoesNotMaskNonSensitiveKey() throws Exception {
        User admin = new User();
        admin.setLoginName(BusinessConstants.DEFAULT_MANAGER);
        admin.setTenantId(null);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(userService.isPlatformSuperAdmin(admin)).thenReturn(true);

        PlatformConfig config = new PlatformConfig();
        config.setId(2L);
        config.setPlatformKey("platform_name");
        config.setPlatformValue("MyERP");
        when(platformConfigMapper.selectByPrimaryKey(2L)).thenReturn(config);

        PlatformConfig result = platformConfigService.getPlatformConfig(2L);

        assertNotNull(result);
        assertEquals("MyERP", result.getPlatformValue());
    }

    @Test
    void selectMasksSensitiveKeysInList() throws Exception {
        User admin = new User();
        admin.setLoginName(BusinessConstants.DEFAULT_MANAGER);
        admin.setTenantId(null);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(userService.isPlatformSuperAdmin(admin)).thenReturn(true);

        // Set up request context for PageUtils.startPage()
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setParameter("page", "1");
        mockRequest.setParameter("pageSize", "10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
        try {
            PlatformConfig publicConfig = new PlatformConfig();
            publicConfig.setPlatformKey("platform_name");
            publicConfig.setPlatformValue("MyERP");
            PlatformConfig sensitiveConfig = new PlatformConfig();
            sensitiveConfig.setPlatformKey("weixinSecret");
            sensitiveConfig.setPlatformValue("real_weixin_secret");
            when(platformConfigMapperEx.selectByConditionPlatformConfig(null))
                    .thenReturn(Arrays.asList(publicConfig, sensitiveConfig));

            List<PlatformConfig> list = platformConfigService.select(null);

            assertEquals(2, list.size());
            assertEquals("MyERP", list.get(0).getPlatformValue());
            assertEquals("******", list.get(1).getPlatformValue());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    // --- updatePlatformConfig field validation tests ---

    @Test
    void updatePlatformConfigRejectsMaskedValueForSensitiveKey() throws Exception {
        User admin = new User();
        admin.setLoginName(BusinessConstants.DEFAULT_MANAGER);
        admin.setTenantId(null);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(userService.isPlatformSuperAdmin(admin)).thenReturn(true);

        PlatformConfig existing = new PlatformConfig();
        existing.setId(1L);
        existing.setPlatformKey("aliOss_accessKeySecret");
        existing.setPlatformValue("real_secret");
        when(platformConfigMapper.selectByPrimaryKey(1L)).thenReturn(existing);

        JSONObject obj = new JSONObject();
        obj.put("id", 1L);
        obj.put("platformValue", "******");

        int result = platformConfigService.updatePlatformConfig(obj, request);

        assertEquals(0, result);
        verify(platformConfigMapper, never()).updateByPrimaryKeySelective(any());
    }

    // --- URL validation tests ---

    @Test
    void updateRejectsInvalidUrl() throws Exception {
        User admin = new User();
        admin.setLoginName(BusinessConstants.DEFAULT_MANAGER);
        admin.setTenantId(null);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(userService.isPlatformSuperAdmin(admin)).thenReturn(true);

        assertThrows(BusinessRunTimeException.class, () -> {
            platformConfigService.updatePlatformConfigByKey("pay_fee_url", "javascript:alert(1)");
        });
    }

    @Test
    void updateAcceptsValidUrl() throws Exception {
        User admin = new User();
        admin.setLoginName(BusinessConstants.DEFAULT_MANAGER);
        admin.setTenantId(null);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(userService.isPlatformSuperAdmin(admin)).thenReturn(true);
        when(platformConfigMapper.updateByExampleSelective(any(), any())).thenReturn(1);

        int result = platformConfigService.updatePlatformConfigByKey("pay_fee_url", "https://pay.example.com/renew");

        assertEquals(1, result);
    }

    // --- Flag validation tests ---

    @Test
    void updateRejectsInvalidFlag() throws Exception {
        User admin = new User();
        admin.setLoginName(BusinessConstants.DEFAULT_MANAGER);
        admin.setTenantId(null);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(userService.isPlatformSuperAdmin(admin)).thenReturn(true);

        assertThrows(BusinessRunTimeException.class, () -> {
            platformConfigService.updatePlatformConfigByKey("register_flag", "abc");
        });
    }

    @Test
    void updateAcceptsValidFlag() throws Exception {
        User admin = new User();
        admin.setLoginName(BusinessConstants.DEFAULT_MANAGER);
        admin.setTenantId(null);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(userService.isPlatformSuperAdmin(admin)).thenReturn(true);
        when(platformConfigMapper.updateByExampleSelective(any(), any())).thenReturn(1);

        assertEquals(1, platformConfigService.updatePlatformConfigByKey("register_flag", "1"));
        assertEquals(1, platformConfigService.updatePlatformConfigByKey("register_flag", "0"));
    }

    // --- Tenant user cannot access any PlatformConfig admin method ---

    @Test
    void tenantUserCannotGetPlatformConfigById() throws Exception {
        User tenantUser = new User();
        tenantUser.setLoginName("tenant_admin");
        tenantUser.setTenantId(100L);
        when(userService.getCurrentUser()).thenReturn(tenantUser);
        when(userService.isPlatformSuperAdmin(tenantUser)).thenReturn(false);

        assertNull(platformConfigService.getPlatformConfig(1L));
        verify(platformConfigMapper, never()).selectByPrimaryKey(any());
    }

    @Test
    void tenantUserCannotListPlatformConfig() throws Exception {
        User tenantUser = new User();
        tenantUser.setLoginName("tenant_admin");
        tenantUser.setTenantId(100L);
        when(userService.getCurrentUser()).thenReturn(tenantUser);
        when(userService.isPlatformSuperAdmin(tenantUser)).thenReturn(false);

        List<PlatformConfig> list = platformConfigService.select(null);

        assertTrue(list.isEmpty());
        verify(platformConfigMapperEx, never()).selectByConditionPlatformConfig(any());
    }
}
