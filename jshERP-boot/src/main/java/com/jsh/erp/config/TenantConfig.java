package com.jsh.erp.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.jsh.erp.utils.Tools;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

@Service
public class TenantConfig {

    /**
     * 需要排除租户过滤的表
     */
    private static final List<String> IGNORE_TABLES = Arrays.asList(
            "jsh_sequence", "jsh_function", "jsh_platform_config",
            "jsh_tenant", "jsh_sys_dict_data", "jsh_sys_dict_type"
    );

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(HttpServletRequest request) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 多租户拦截器（必须在分页拦截器之前）
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                String token = request.getHeader("X-Access-Token");
                Long tenantId = Tools.getTenantIdByToken(token);
                if (tenantId != 0L) {
                    return new LongValue(tenantId);
                } else {
                    // 超管，返回 null 表示不过滤
                    return new NullValue();
                }
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return IGNORE_TABLES.contains(tableName);
            }
        }));

        // 分页拦截器（必须在多租户拦截器之后）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());

        return interceptor;
    }

    /**
     * 相当于顶部的：
     * {@code @MapperScan("com.jsh.erp.datasource.mappers*")}
     * 这里可以扩展，比如使用配置文件来配置扫描Mapper的路径
     */
    @Bean
    public MapperScannerConfigurer mapperScannerConfigurer() {
        MapperScannerConfigurer scannerConfigurer = new MapperScannerConfigurer();
        scannerConfigurer.setBasePackage("com.jsh.erp.datasource.mappers*");
        return scannerConfigurer;
    }
}
