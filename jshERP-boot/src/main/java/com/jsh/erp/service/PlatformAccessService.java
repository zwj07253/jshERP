package com.jsh.erp.service;

import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.exception.BusinessRunTimeException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 平台管理员访问控制服务
 * 禁止平台管理员（admin, tenant_id=0）写入租户业务数据
 */
@Service
public class PlatformAccessService {

    @Resource
    private UserService userService;

    /**
     * 断言当前用户允许写入租户业务数据
     * 平台管理员调用时将抛出 BusinessRunTimeException
     */
    public void assertBusinessWriteAllowed() throws Exception {
        User user = userService.getCurrentUser();
        if (user != null && userService.isPlatformSuperAdmin(user)) {
            throw new BusinessRunTimeException(
                    ExceptionConstants.PLATFORM_ADMIN_BUSINESS_WRITE_FORBIDDEN_CODE,
                    ExceptionConstants.PLATFORM_ADMIN_BUSINESS_WRITE_FORBIDDEN_MSG);
        }
    }
}
