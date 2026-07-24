package com.jsh.erp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在 Controller 方法上，表示该接口需要租户开通指定的功能模块才能访问。
 * 例如 @RequireTenantFeature("TRADE") 表示需要外贸模块。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireTenantFeature {
    String value();
}
