package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.entities.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Global AI import configuration. The API token is encrypted and never returned to a client. */
@Service
public class AiModelConfigService {
    private static final String PREFIX = "v1:";
    @Resource private JdbcTemplate jdbcTemplate;
    @Resource private UserService userService;
    @Value("${ai.config.master-key:${AI_CONFIG_MASTER_KEY:}}") private String masterKey;

    public Map<String, Object> getMaskedConfig() throws Exception {
        assertAdministrator();
        Config c = load();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", c.enabled); result.put("apiFormat", c.apiFormat); result.put("apiUrl", c.apiUrl); result.put("modelName", c.modelName);
        result.put("timeoutSeconds", c.timeoutSeconds); result.put("maxFileMb", c.maxFileMb);
        result.put("visionEnabled", c.visionEnabled); result.put("customPrompt", c.customPrompt);
        result.put("apiTokenConfigured", !blank(c.encryptedToken));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> save(JSONObject input) throws Exception {
        assertAdministrator();
        Config old = load();
        String token = input.getString("apiToken");
        String encrypted = blank(token) ? old.encryptedToken : encrypt(token.trim());
        boolean enabled = Boolean.TRUE.equals(input.getBoolean("enabled"));
        String apiFormat = trim(input.getString("apiFormat")).toUpperCase();
        if (!"OPENAI".equals(apiFormat) && !"ANTHROPIC".equals(apiFormat)) apiFormat = "OPENAI";
        String apiUrl = trim(input.getString("apiUrl"));
        String modelName = trim(input.getString("modelName"));
        int timeout = range(input.getInteger("timeoutSeconds"), 15, 180, 60);
        int maxFileMb = range(input.getInteger("maxFileMb"), 1, 20, 10);
        boolean vision = Boolean.TRUE.equals(input.getBoolean("visionEnabled"));
        String prompt = trim(input.getString("customPrompt"));
        if (prompt.length() > 4000) prompt = prompt.substring(0, 4000);
        if (enabled && (blank(apiUrl) || blank(modelName) || blank(encrypted))) {
            throw new IllegalArgumentException("启用 AI 导入前，请填写 API 地址、模型名称和 API Token");
        }
        int updated = jdbcTemplate.update("update jsh_ai_config set enabled=?,api_format=?,api_url=?,model_name=?,encrypted_token=?,timeout_seconds=?,max_file_mb=?,vision_enabled=?,custom_prompt=?,updated_time=current_timestamp where id=1", enabled, apiFormat, apiUrl, modelName, encrypted, timeout, maxFileMb, vision, prompt);
        if (updated == 0) jdbcTemplate.update("insert into jsh_ai_config(id,enabled,api_format,api_url,model_name,encrypted_token,timeout_seconds,max_file_mb,vision_enabled,custom_prompt,updated_time) values(1,?,?,?,?,?,?,?,?,?,current_timestamp)", enabled, apiFormat, apiUrl, modelName, encrypted, timeout, maxFileMb, vision, prompt);
        return getMaskedConfig();
    }

    public Config getRuntimeConfig() throws Exception {
        userService.getCurrentUser();
        Config c = load();
        if (!c.enabled) throw new IllegalStateException("AI 导入尚未启用，请联系系统管理员完成配置");
        if (blank(c.apiUrl) || blank(c.modelName) || blank(c.encryptedToken)) throw new IllegalStateException("AI 模型配置不完整");
        c.apiToken = decrypt(c.encryptedToken); return c;
    }

    public void assertAdministrator() throws Exception {
        User user = userService.getCurrentUser();
        Role role = user == null ? null : userService.getRoleTypeByUserId(user.getId());
        boolean manager = user != null && ("admin".equalsIgnoreCase(user.getLoginName()) || "jsh".equalsIgnoreCase(user.getLoginName()));
        boolean systemRole = role != null && "系统管理员".equals(role.getName());
        if (!manager && !systemRole) {
            throw new SecurityException("只有系统管理员可以配置 AI 导入");
        }
    }

    private Config load() {
        try {
            List<Map<String,Object>> rows = jdbcTemplate.queryForList("select enabled,api_format,api_url,model_name,encrypted_token,timeout_seconds,max_file_mb,vision_enabled,custom_prompt from jsh_ai_config where id=1");
            if (rows.isEmpty()) return new Config(); Map<String,Object> r = rows.get(0); Config c = new Config();
            c.enabled = bool(r.get("enabled")); c.apiFormat = str(r.get("api_format")); if (blank(c.apiFormat)) c.apiFormat = "OPENAI"; c.apiUrl = str(r.get("api_url")); c.modelName = str(r.get("model_name")); c.encryptedToken = str(r.get("encrypted_token"));
            c.timeoutSeconds = integer(r.get("timeout_seconds"), 60); c.maxFileMb = integer(r.get("max_file_mb"), 10); c.visionEnabled = bool(r.get("vision_enabled")); c.customPrompt = str(r.get("custom_prompt")); return c;
        } catch (Exception e) { throw new IllegalStateException("AI 配置表不存在，请执行 docs/ai_import.sql", e); }
    }
    private String encrypt(String value) throws Exception { byte[] iv = new byte[12]; new SecureRandom().nextBytes(iv); Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv)); byte[] payload = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)); byte[] all = new byte[iv.length + payload.length]; System.arraycopy(iv,0,all,0,iv.length); System.arraycopy(payload,0,all,iv.length,payload.length); return PREFIX + Base64.getEncoder().encodeToString(all); }
    private String decrypt(String value) throws Exception { if (blank(value) || !value.startsWith(PREFIX)) throw new IllegalStateException("AI Token 格式无效，请由管理员重新保存"); byte[] all = Base64.getDecoder().decode(value.substring(PREFIX.length())); if (all.length <= 12) throw new IllegalStateException("AI Token 格式无效"); byte[] iv = java.util.Arrays.copyOfRange(all,0,12); byte[] payload = java.util.Arrays.copyOfRange(all,12,all.length); Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,iv)); return new String(cipher.doFinal(payload),StandardCharsets.UTF_8); }
    private SecretKeySpec key() throws Exception { if (blank(masterKey) || masterKey.trim().length() < 16) throw new IllegalStateException("未配置 AI_CONFIG_MASTER_KEY（至少 16 个字符）"); return new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(masterKey.trim().getBytes(StandardCharsets.UTF_8)),"AES"); }
    private boolean bool(Object v) { return v != null && (Boolean.TRUE.equals(v) || "1".equals(String.valueOf(v)) || "true".equalsIgnoreCase(String.valueOf(v))); }
    private int integer(Object v,int d) { return v instanceof Number ? ((Number)v).intValue() : d; }
    private int range(Integer v,int min,int max,int d) { return Math.max(min,Math.min(max,v == null ? d : v)); }
    private String str(Object v) { return v == null ? "" : String.valueOf(v); }
    private String trim(String v) { return v == null ? "" : v.trim(); }
    private boolean blank(String v) { return v == null || v.trim().isEmpty(); }
    public static class Config { public boolean enabled; public String apiFormat="OPENAI", apiUrl="", modelName="", apiToken="", encryptedToken="", customPrompt=""; public int timeoutSeconds=60,maxFileMb=10; public boolean visionEnabled; }
}
