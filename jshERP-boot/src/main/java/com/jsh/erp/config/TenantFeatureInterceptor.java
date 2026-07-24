package com.jsh.erp.config;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.annotation.RequireTenantFeature;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.service.TenantFeatureMappingService;
import com.jsh.erp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Set;

/**
 * 拦截器：校验当前请求的租户是否开通了指定的功能模块。
 * 配合 @RequireTenantFeature 注解使用。
 */
@Component
public class TenantFeatureInterceptor implements HandlerInterceptor {
    private Logger logger = LoggerFactory.getLogger(TenantFeatureInterceptor.class);

    @Resource
    private TenantFeatureMappingService tenantFeatureMappingService;

    @Resource
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireTenantFeature annotation = handlerMethod.getMethodAnnotation(RequireTenantFeature.class);
        if (annotation == null) {
            return true;
        }
        String requiredFeature = annotation.value();

        try {
            // 平台管理员跳过检查
            User currentUser = userService.getCurrentUser();
            if (currentUser != null && "admin".equals(currentUser.getLoginName()) && currentUser.getTenantId() == null) {
                return true;
            }
            // 检查租户是否开通了该功能模块
            Long tenantId = currentUser != null ? currentUser.getTenantId() : null;
            if (tenantId != null) {
                Set<String> featureCodes = tenantFeatureMappingService.getTenantFeatureCodes(tenantId);
                if (featureCodes != null && featureCodes.contains(requiredFeature)) {
                    return true;
                }
            }
            // 租户未开通该功能模块
            logger.warn("租户功能模块访问被拒绝: tenantId={}, requiredFeature={}", tenantId, requiredFeature);
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter writer = response.getWriter();
            JSONObject result = new JSONObject();
            result.put("code", 403);
            result.put("data", "该功能模块未开通，请联系管理员");
            writer.write(result.toJSONString());
            writer.flush();
            return false;
        } catch (Exception e) {
            logger.error("租户功能模块校验异常", e);
            return true;
        }
    }
}
