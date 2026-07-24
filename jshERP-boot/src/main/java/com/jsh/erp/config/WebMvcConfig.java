package com.jsh.erp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private TenantFeatureInterceptor tenantFeatureInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantFeatureInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/user/weixinLogin",
                        "/user/weixinBind",
                        "/user/registerUser",
                        "/user/randomImage",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**"
                );
    }
}
