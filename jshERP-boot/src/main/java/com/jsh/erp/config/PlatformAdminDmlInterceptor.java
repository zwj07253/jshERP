package com.jsh.erp.config;

import com.jsh.erp.utils.Tools;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.*;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * 平台管理员 DML 守卫拦截器。
 * 当当前用户是平台管理员（tenant_id=0）时，拦截对业务表的 INSERT/UPDATE/DELETE，
 * 仅放行平台表及明确的平台业务流程。
 */
@Component
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class PlatformAdminDmlInterceptor implements Interceptor {

    /**
     * 允许平台管理员写入的表（白名单）
     */
    private static final Set<String> PLATFORM_TABLES = new HashSet<>(Arrays.asList(
            "jsh_platform_config",
            "jsh_tenant",
            "jsh_function",
            "jsh_role",
            "jsh_user",
            "jsh_user_business",
            "jsh_log",
            "jsh_sequence",
            "jsh_sys_dict_data",
            "jsh_sys_dict_type",
            "jsh_plugin"
    ));

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler handler = (StatementHandler) invocation.getTarget();
        BoundSql boundSql = handler.getBoundSql();
        String sql = boundSql.getSql().trim();

        // 快速判断：只拦截 DML 语句
        String upperSql = sql.toUpperCase();
        boolean isDml = upperSql.startsWith("INSERT") || upperSql.startsWith("UPDATE") || upperSql.startsWith("DELETE");
        if (!isDml) {
            return invocation.proceed();
        }

        // 检查当前用户是否为平台管理员
        String token = null;
        try {
            // 通过 RequestContextHolder 获取 token（与 TenantConfig 保持一致）
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                token = attrs.getRequest().getHeader("X-Access-Token");
            }
        } catch (Exception ignored) {
        }

        if (token == null) {
            return invocation.proceed();
        }

        Long tenantId = Tools.getTenantIdByToken(token);
        if (tenantId == null || tenantId != 0L) {
            return invocation.proceed();
        }

        // 平台管理员执行 DML，检查目标表
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            String tableName = extractTableName(statement);
            if (tableName != null && !PLATFORM_TABLES.contains(tableName.toLowerCase())) {
                throw new RuntimeException("平台管理员不可修改租户业务数据（表: " + tableName + "），请使用租户管理员账号");
            }
        } catch (JSQLParserException e) {
            // SQL 解析失败时拒绝放行，防止绕过
            throw new RuntimeException("平台管理员执行的SQL无法解析，已拦截: " + e.getMessage(), e);
        }

        return invocation.proceed();
    }

    private String extractTableName(Statement statement) {
        if (statement instanceof Insert) {
            return ((Insert) statement).getTable().getName();
        } else if (statement instanceof Update) {
            return ((Update) statement).getTable().getName();
        } else if (statement instanceof Delete) {
            return ((Delete) statement).getTable().getName();
        }
        return null;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
