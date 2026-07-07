package com.jsh.erp.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

/**
 * PostgreSQL 兼容的多租户拦截器。
 * 解决多表 JOIN 时 tenant_id 列歧义问题：
 * 原版 getAliasColumn 在表没有别名时只返回裸 "tenant_id"，
 * PostgreSQL 严格模式下多个表都有 tenant_id 时会报歧义。
 * 此处重写：没有别名时使用表全名作为前缀。
 */
public class PostgresTenantLineInnerInterceptor extends TenantLineInnerInterceptor {

    private final TenantLineHandler handler;

    public PostgresTenantLineInnerInterceptor(TenantLineHandler tenantLineHandler) {
        super(tenantLineHandler);
        this.handler = tenantLineHandler;
    }

    @Override
    protected Column getAliasColumn(Table table) {
        String tenantIdColumn = handler.getTenantIdColumn();
        if (table.getAlias() != null && table.getAlias().getName() != null) {
            return new Column(table.getAlias().getName() + "." + tenantIdColumn);
        }
        // 没有别名时，用表全名限定列名，避免 PostgreSQL 列歧义
        return new Column(table.getName() + "." + tenantIdColumn);
    }
}
