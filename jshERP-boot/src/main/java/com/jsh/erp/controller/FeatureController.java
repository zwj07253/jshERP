package com.jsh.erp.controller;

import com.jsh.erp.datasource.entities.Feature;
import com.jsh.erp.service.FeatureService;
import com.jsh.erp.service.UserService;
import com.jsh.erp.utils.BaseResponseInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@Tag(name = "功能模块管理")
@RequestMapping("/feature")
public class FeatureController {
    private Logger logger = LoggerFactory.getLogger(FeatureController.class);

    @Resource
    private FeatureService featureService;

    @Resource
    private UserService userService;

    @GetMapping("/getAllList")
    @Operation(summary = "获取所有功能模块")
    public BaseResponseInfo getAllList() throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            List<Feature> list = featureService.getAllFeatures();
            res.code = 200;
            res.data = list;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "查询功能模块失败";
        }
        return res;
    }

    @PostMapping("/add")
    @Operation(summary = "新增功能模块")
    public BaseResponseInfo add(@RequestBody Feature feature, HttpServletRequest request) throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            featureService.insertFeature(feature);
            res.code = 200;
            res.data = "新增成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "新增功能模块失败";
        }
        return res;
    }

    @PostMapping("/update")
    @Operation(summary = "编辑功能模块")
    public BaseResponseInfo update(@RequestBody Feature feature, HttpServletRequest request) throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            featureService.updateFeature(feature);
            res.code = 200;
            res.data = "编辑成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "编辑功能模块失败";
        }
        return res;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除功能模块")
    public BaseResponseInfo delete(@RequestParam("id") Long id, HttpServletRequest request) throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            featureService.deleteFeature(id);
            res.code = 200;
            res.data = "删除成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "删除功能模块失败";
        }
        return res;
    }
}
