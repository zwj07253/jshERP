package com.jsh.erp.config;

import com.gitee.starblues.integration.application.AutoPluginApplication;
import com.gitee.starblues.integration.application.PluginApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PluginBeanConfig {
    @Bean
    public PluginApplication pluginApplication(){
        return new AutoPluginApplication();
    }
}
