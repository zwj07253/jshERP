package com.jsh.erp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 配置
 *
 * @author jishenghua
 * @version 2.0
 */
@Configuration
public class Swagger2Config {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("管伊佳ERP Restful Api")
                        .description("管伊佳ERP接口描述")
                        .version("3.0")
                        .contact(new Contact().name("jishenghua")));
    }
}
