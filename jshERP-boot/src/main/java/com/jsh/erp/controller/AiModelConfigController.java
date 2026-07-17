package com.jsh.erp.controller;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.base.BaseController;
import com.jsh.erp.service.AiInventoryParserService;
import com.jsh.erp.service.AiModelConfigService;
import com.jsh.erp.utils.BaseResponseInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai/config/inventory")
public class AiModelConfigController extends BaseController {

    @Resource
    private AiModelConfigService configService;

    @Resource
    private AiInventoryParserService parserService;

    @GetMapping
    public BaseResponseInfo getConfig() {
        return execute(() -> configService.getMaskedConfig());
    }

    @PutMapping
    public BaseResponseInfo save(@RequestBody JSONObject input) {
        return execute(() -> configService.save(input));
    }

    @GetMapping("/test")
    public BaseResponseInfo test() {
        return execute(() -> {
            AiModelConfigService.Config config = configService.getRuntimeConfigForAdmin();
            String reply = parserService.testConnection(config);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "连接成功");
            result.put("reply", reply);
            return result;
        });
    }

    private BaseResponseInfo execute(CheckedSupplier supplier) {
        BaseResponseInfo response = new BaseResponseInfo();
        try {
            response.code = 200;
            response.data = supplier.get();
        } catch (Exception e) {
            logger.error("AI 模型配置操作失败", e);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("message", e.getMessage() == null ? "AI 模型配置操作失败" : e.getMessage());
            response.code = 500;
            response.data = data;
        }
        return response;
    }

    private interface CheckedSupplier {
        Object get() throws Exception;
    }
}
