package com.jsh.erp.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.base.BaseController;
import com.jsh.erp.service.AiInventoryParserService;
import com.jsh.erp.service.AiModelConfigService;
import com.jsh.erp.service.TradeInventoryImportService;
import com.jsh.erp.service.UserService;
import com.jsh.erp.utils.BaseResponseInfo;
import com.jsh.erp.utils.Tools;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/trade/inventory/ai")
public class TradeInventoryAiController extends BaseController {

    @Resource
    private AiModelConfigService configService;
    @Resource
    private AiInventoryParserService parserService;
    @Resource
    private TradeInventoryImportService importService;
    @Resource
    private UserService userService;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/parse")
    public BaseResponseInfo parse(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        long started = System.currentTimeMillis();
        try {
            userService.getCurrentUser();
            Long tenantId = resolveTenantId(request);
            AiModelConfigService.Config config = configService.getRuntimeConfig();
            JSONObject aiResult = parserService.parse(file, config);
            List<Map<String, Object>> sourceRows = toRows(aiResult.getJSONArray("rows"));
            List<Map<String, Object>> rows = importService.preview(sourceRows, tenantId);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("rows", rows);
            data.put("warnings", aiResult.getJSONArray("warnings"));
            data.put("model", config.modelName);
            data.put("elapsedMs", System.currentTimeMillis() - started);
            logImportQuietly(request, tenantId, file, config.modelName, "PARSED", rows.size(), 0, null);
            return apiSuccess(data);
        } catch (Exception e) {
            logger.error("外贸库存 AI 解析失败", e);
            logImportQuietly(request, resolveTenantId(request), file, "", "FAILED", 0, 0, e.getMessage());
            return apiFailure(e.getMessage());
        }
    }

    @PostMapping("/confirm")
    public BaseResponseInfo confirm(@RequestBody JSONObject input, HttpServletRequest request) {
        try {
            userService.getCurrentUser();
            Long tenantId = resolveTenantId(request);
            List<Map<String, Object>> rows = toRows(input.getJSONArray("rows"));
            List<Map<String, Object>> saved = importService.confirm(rows, tenantId);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("rows", saved);
            data.put("count", saved.size());
            logImportQuietly(request, tenantId, null, "", "IMPORTED", rows.size(), saved.size(), null);
            return apiSuccess(data);
        } catch (Exception e) {
            logger.error("外贸库存 AI 确认导入失败", e);
            return apiFailure(e.getMessage());
        }
    }

    private List<Map<String, Object>> toRows(JSONArray array) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (array == null) return rows;
        for (Object item : array) {
            JSONObject object = item instanceof JSONObject ? (JSONObject) item : JSON.parseObject(JSON.toJSONString(item));
            rows.add(new LinkedHashMap<>(object));
        }
        return rows;
    }

    private BaseResponseInfo apiSuccess(Object data) {
        BaseResponseInfo response = new BaseResponseInfo();
        response.code = 200;
        response.data = data;
        return response;
    }

    private BaseResponseInfo apiFailure(String message) {
        BaseResponseInfo response = new BaseResponseInfo();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", message == null ? "AI 解析失败" : message);
        response.code = 500;
        response.data = data;
        return response;
    }

    private Long resolveTenantId(HttpServletRequest request) {
        Long tenantId = Tools.getTenantIdByToken(request.getHeader("X-Access-Token"));
        if (tenantId != null && tenantId > 0) return tenantId;
        try {
            Long demoTenant = jdbcTemplate.queryForObject(
                    "select coalesce(min(tenant_id),0) from jsh_trade_shipment where coalesce(delete_flag,'0')<>'1'", Long.class);
            return demoTenant == null ? 0L : demoTenant;
        } catch (Exception e) {
            return 0L;
        }
    }

    private void logImport(HttpServletRequest request, Long tenantId, MultipartFile file, String model,
                           String status, int parsedCount, int successCount, String error) throws Exception {
        Long userId = userService.getUserId(request);
        String fileName = file == null ? null : trim(file.getOriginalFilename(), 255);
        jdbcTemplate.update(
                "insert into jsh_ai_import_log (user_id,tenant_id,file_name,model_name,status,parsed_count,success_count,error_message,created_time) values (?,?,?,?,?,?,?,?,current_timestamp)",
                userId, tenantId, fileName, trim(model, 100), status, parsedCount, successCount, trim(error, 1000));
    }

    private void logImportQuietly(HttpServletRequest request, Long tenantId, MultipartFile file, String model,
                                  String status, int parsedCount, int successCount, String error) {
        try {
            logImport(request, tenantId, file, model, status, parsedCount, successCount, error);
        } catch (Exception ignored) {
            // 数据库迁移未执行时，不能让日志写入掩盖真正的解析错误。
        }
    }

    private String trim(String value, int maxLength) {
        if (value == null) return null;
        String result = value.trim();
        return result.length() > maxLength ? result.substring(0, maxLength) : result;
    }
}
