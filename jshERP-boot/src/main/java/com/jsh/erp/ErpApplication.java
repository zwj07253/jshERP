package com.jsh.erp;

import com.jsh.erp.utils.ComputerInfo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.jsh.erp.datasource.mappers")
@ServletComponentScan
@EnableScheduling
@EnableAsync
public class ErpApplication{
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ErpApplication.class, args);
        Environment environment = context.getBean(Environment.class);
        System.out.println("启动成功，后端服务API地址：http://" + ComputerInfo.getIpAddr() + ":"
                + environment.getProperty("server.port") + "/jshERP-boot/doc.html");
    }
}
