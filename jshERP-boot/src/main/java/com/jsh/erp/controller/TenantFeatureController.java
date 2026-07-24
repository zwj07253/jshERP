package com.jsh.erp.controller;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.service.TenantFeatureMappingService;
import com.jsh.erp.utils.BaseResponseInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "租户功能授权")
@RequestMapping("/tenantFeature")
public class TenantFeatureController {
    private Logger logger = LoggerFactory.getLogger(TenantFeatureController.class);

    @Resource
    private TenantFeatureMappingService tenantFeatureMappingService;

    @GetMapping("/getByTenantId")
    @Operation(summary = "获取租户功能授权列表")
    public BaseResponseInfo getByTenantId(@RequestParam("tenantId") Long tenantId) throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            List<Map<String, Object>> list = tenantFeatureMappingService.getTenantFeatures(tenantId);
            res.code = 200;
            res.data = list;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "查询租户功能授权失败";
        }
        return res;
    }

    @PostMapping("/batchUpdate")
    @Operation(summary = "批量更新租户功能授权")
    public BaseResponseInfo batchUpdate(@RequestBody JSONObject jsonObject) throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Long tenantId = jsonObject.getLong("tenantId");
            List<Long> featureIds = jsonObject.getList("featureIds", Long.class);
            tenantFeatureMappingService.batchUpdateTenantFeatures(tenantId, featureIds);
            res.code = 200;
            res.data = "更新成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "更新租户功能授权失败";
        }
        return res;
    }
}
