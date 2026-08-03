package com.jsh.erp.controller;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.base.BaseController;
import com.jsh.erp.service.AiDocumentParserService;
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
@RequestMapping("/ai/config")
public class AiModelConfigController extends BaseController {
    @Resource private AiModelConfigService configService; @Resource private AiDocumentParserService parserService;
    @GetMapping public BaseResponseInfo get(){return run(() -> configService.getMaskedConfig());}
    @PutMapping public BaseResponseInfo save(@RequestBody JSONObject input){return run(() -> configService.save(input));}
    @GetMapping("/test") public BaseResponseInfo test(){return run(() -> { configService.assertAdministrator(); String reply=parserService.test(configService.getRuntimeConfig()); Map<String,Object> data=new LinkedHashMap<>();data.put("message","连接成功");data.put("reply",reply);return data;});}
    private BaseResponseInfo run(Action action){BaseResponseInfo r=new BaseResponseInfo();try{r.code=200;r.data=action.get();}catch(Exception e){logger.error("AI config failed",e);r.code=500;Map<String,Object>d=new LinkedHashMap<>();d.put("message",e.getMessage());r.data=d;}return r;} private interface Action{Object get() throws Exception;}
}
