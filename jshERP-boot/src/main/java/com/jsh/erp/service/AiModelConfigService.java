package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.datasource.entities.Role;
import com.jsh.erp.datasource.entities.User;
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

/**
 * 外贸库存 AI 模型配置。Token 只在服务端解密，任何接口都不会返回明文。
 */
@Service
public class AiModelConfigService {

    private static final String CIPHER_PREFIX = "v1:";
    private static final int IV_LENGTH = 12;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private UserService userService;

    @Value("${ai.config.master-key:${AI_CONFIG_MASTER_KEY:}}")
    private String masterKey;

    public Map<String, Object> getMaskedConfig() throws Exception {
        assertAdministrator();
        Config config = loadConfig();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", config.enabled);
        result.put("provider", config.provider);
        result.put("apiUrl", config.apiUrl);
        result.put("modelName", config.modelName);
        result.put("timeoutSeconds", config.timeoutSeconds);
        result.put("maxFileMb", config.maxFileMb);
        result.put("visionEnabled", config.visionEnabled);
        result.put("customPrompt", config.customPrompt);
        result.put("apiTokenConfigured", !isBlank(config.encryptedToken));
        result.put("apiTokenMasked", maskToken(config.encryptedToken));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> save(JSONObject input) throws Exception {
        assertAdministrator();
        Config old = loadConfig();
        String token = input.getString("apiToken");
        String encryptedToken = isBlank(token) ? old.encryptedToken : encrypt(token.trim());
        boolean enabled = Boolean.TRUE.equals(input.getBoolean("enabled"));
        String provider = defaultString(input.getString("provider"), "OpenAI Compatible");
        String apiUrl = trim(input.getString("apiUrl"));
        String modelName = trim(input.getString("modelName"));
        int timeoutSeconds = intRange(input.getInteger("timeoutSeconds"), 15, 180, 60);
        int maxFileMb = intRange(input.getInteger("maxFileMb"), 1, 20, 10);
        boolean visionEnabled = Boolean.TRUE.equals(input.getBoolean("visionEnabled"));
        String customPrompt = trimToLength(input.getString("customPrompt"), 4000);

        if (enabled && (isBlank(apiUrl) || isBlank(modelName) || isBlank(encryptedToken))) {
            throw new IllegalArgumentException("启用 AI 导入前，请填写 API 地址、模型名称和 API Token");
        }

        int updated = jdbcTemplate.update(
                "update jsh_ai_config set enabled=?,provider=?,api_url=?,model_name=?,encrypted_token=?,timeout_seconds=?,max_file_mb=?,vision_enabled=?,custom_prompt=?,updated_by=?,updated_time=current_timestamp where id=1",
                enabled, provider, apiUrl, modelName, encryptedToken, timeoutSeconds, maxFileMb,
                visionEnabled, customPrompt, userService.getCurrentUser().getLoginName());
        if (updated == 0) {
            jdbcTemplate.update(
                    "insert into jsh_ai_config (id,enabled,provider,api_url,model_name,encrypted_token,timeout_seconds,max_file_mb,vision_enabled,custom_prompt,updated_by,updated_time) values (1,?,?,?,?,?,?,?,?,?,?,current_timestamp)",
                    enabled, provider, apiUrl, modelName, encryptedToken, timeoutSeconds, maxFileMb,
                    visionEnabled, customPrompt, userService.getCurrentUser().getLoginName());
        }
        return getMaskedConfig();
    }

    public Config getRuntimeConfig() throws Exception {
        userService.getCurrentUser();
        Config config = loadConfig();
        if (!config.enabled) {
            throw new IllegalStateException("AI 导入尚未启用，请联系系统管理员完成配置");
        }
        if (isBlank(config.apiUrl) || isBlank(config.modelName) || isBlank(config.encryptedToken)) {
            throw new IllegalStateException("AI 模型配置不完整，请联系系统管理员检查 API 地址、模型和 Token");
        }
        config.apiToken = decrypt(config.encryptedToken);
        return config;
    }

    public Config getRuntimeConfigForAdmin() throws Exception {
        assertAdministrator();
        Config config = loadConfig();
        if (isBlank(config.apiUrl) || isBlank(config.modelName) || isBlank(config.encryptedToken)) {
            throw new IllegalStateException("请先保存 API 地址、模型名称和 API Token");
        }
        config.apiToken = decrypt(config.encryptedToken);
        return config;
    }

    public void assertAdministrator() throws Exception {
        User user = userService.getCurrentUser();
        if (BusinessConstants.DEFAULT_MANAGER.equals(user.getLoginName())) {
            return;
        }
        Role role = userService.getRoleTypeByUserId(user.getId());
        boolean platformAdministrator = role != null && role.getId() != null && role.getId() == 4L;
        boolean tenantAdministrator = role != null && "系统管理员".equals(role.getName());
        if (!platformAdministrator && !tenantAdministrator) {
            throw new SecurityException("只有系统管理员可以查看或修改 AI 模型配置");
        }
    }

    private Config loadConfig() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "select enabled,provider,api_url,model_name,encrypted_token,timeout_seconds,max_file_mb,vision_enabled,custom_prompt from jsh_ai_config where id=1");
            if (rows.isEmpty()) return new Config();
            Map<String, Object> row = rows.get(0);
            Config config = new Config();
            config.enabled = bool(row.get("enabled"));
            config.provider = string(row.get("provider"), "OpenAI Compatible");
            config.apiUrl = string(row.get("api_url"), "");
            config.modelName = string(row.get("model_name"), "");
            config.encryptedToken = string(row.get("encrypted_token"), "");
            config.timeoutSeconds = number(row.get("timeout_seconds"), 60);
            config.maxFileMb = number(row.get("max_file_mb"), 10);
            config.visionEnabled = bool(row.get("vision_enabled"));
            config.customPrompt = string(row.get("custom_prompt"), "");
            return config;
        } catch (Exception e) {
            throw new IllegalStateException("AI 配置表不存在，请先执行 jshERP-boot/docs/jsh_erp_trade_pg.sql 更新数据库", e);
        }
    }

    private String encrypt(String value) throws Exception {
        SecretKeySpec key = encryptionKey();
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        return CIPHER_PREFIX + Base64.getEncoder().encodeToString(combined);
    }

    private String decrypt(String value) throws Exception {
        if (isBlank(value) || !value.startsWith(CIPHER_PREFIX)) {
            throw new IllegalStateException("AI Token 格式无效，请由管理员重新保存 Token");
        }
        byte[] combined = Base64.getDecoder().decode(value.substring(CIPHER_PREFIX.length()));
        byte[] iv = new byte[IV_LENGTH];
        byte[] encrypted = new byte[combined.length - IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
        System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private SecretKeySpec encryptionKey() throws Exception {
        if (isBlank(masterKey) || masterKey.trim().length() < 16) {
            throw new IllegalStateException("未配置 AI_CONFIG_MASTER_KEY，至少需要 16 个字符，配置后请重启后端服务");
        }
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(masterKey.trim().getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, "AES");
    }

    private String maskToken(String encryptedToken) {
        return isBlank(encryptedToken) ? "" : "••••••••（已安全保存）";
    }

    private boolean bool(Object value) {
        return value != null && (Boolean.TRUE.equals(value) || "1".equals(String.valueOf(value)) || "true".equalsIgnoreCase(String.valueOf(value)));
    }

    private int number(Object value, int defaultValue) {
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    private int intRange(Integer value, int min, int max, int defaultValue) {
        int result = value == null ? defaultValue : value;
        return Math.max(min, Math.min(max, result));
    }

    private String string(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToLength(String value, int maxLength) {
        String result = trim(value);
        return result.length() > maxLength ? result.substring(0, maxLength) : result;
    }

    private String defaultString(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class Config {
        public boolean enabled;
        public String provider = "OpenAI Compatible";
        public String apiUrl = "";
        public String modelName = "";
        public String apiToken = "";
        public String encryptedToken = "";
        public int timeoutSeconds = 60;
        public int maxFileMb = 10;
        public boolean visionEnabled;
        public String customPrompt = "";
    }
}
