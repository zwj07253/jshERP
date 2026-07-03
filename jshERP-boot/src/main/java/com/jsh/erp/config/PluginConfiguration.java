package com.jsh.erp.config;

import com.gitee.starblues.core.RuntimeMode;
import com.gitee.starblues.integration.DefaultIntegrationConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "plugin")
public class PluginConfiguration extends DefaultIntegrationConfiguration {

    @Value("${runMode:dev}")
    private String runMode;

    @Value("${pluginPath:plugins}")
    private String pluginPath;

    @Override
    public RuntimeMode environment() {
        return RuntimeMode.byName(runMode);
    }

    @Override
    public String mainPackage() {
        return "com.jsh.erp";
    }

    @Override
    public List<String> pluginPath() {
        return Collections.singletonList(pluginPath);
    }

    @Override
    public String uploadTempPath() {
        return "temp";
    }

    @Override
    public String backupPath() {
        return "backupPlugin";
    }

    @Override
    public String pluginRestPathPrefix() {
        return "/api/plugin";
    }

    @Override
    public Boolean enablePluginIdRestPathPrefix() {
        return true;
    }

    public String getRunMode() {
        return runMode;
    }

    public void setRunMode(String runMode) {
        this.runMode = runMode;
    }

    public String getPluginPath() {
        return pluginPath;
    }

    public void setPluginPath(String pluginPath) {
        this.pluginPath = pluginPath;
    }
}
