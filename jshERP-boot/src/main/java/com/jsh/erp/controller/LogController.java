package com.jsh.erp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.jsh.erp.base.BaseController;
import com.jsh.erp.base.TableDataInfo;
import com.jsh.erp.datasource.entities.Log;
import com.jsh.erp.datasource.vo.LogVo4List;
import com.jsh.erp.service.LogService;
import com.jsh.erp.utils.Constants;
import com.jsh.erp.utils.ErpInfo;
import com.jsh.erp.utils.StringUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jsh.erp.utils.ResponseJsonUtil.returnJson;

/**
 * @author ji sheng hua 752*718*920
 */
@RestController
@RequestMapping(value = "/log")
@Tag(name = "日志管理")
public class LogController extends BaseController {
    @Resource
    private LogService logService;


    @GetMapping(value = "/info")
    @Operation(summary = "根据id获取信息")
    public String getList(@RequestParam("id") Long id,
                          HttpServletRequest request) throws Exception {
        logService.checkReadPermission();
        Log log = logService.getLog(id);
        Map<String, Object> objectMap = new HashMap<>();
        if(log != null) {
            objectMap.put("info", log);
            return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
        } else {
            return returnJson(objectMap, ErpInfo.ERROR.name, ErpInfo.ERROR.code);
        }
    }

    @GetMapping(value = "/list")
    @Operation(summary = "获取信息列表")
    public TableDataInfo getList(@RequestParam(value = Constants.SEARCH, required = false) String search,
                                 HttpServletRequest request)throws Exception {
        logService.checkReadPermission();
        String operation = StringUtil.getInfo(search, "operation");
        String userInfo = StringUtil.getInfo(search, "userInfo");
        String clientIp = StringUtil.getInfo(search, "clientIp");
        String tenantLoginName = StringUtil.getInfo(search, "tenantLoginName");
        String tenantType = StringUtil.getInfo(search, "tenantType");
        String beginTime = StringUtil.getInfo(search, "beginTime");
        String endTime = StringUtil.getInfo(search, "endTime");
        String content = StringUtil.getInfo(search, "content");
        List<LogVo4List> list = logService.select(operation, userInfo, clientIp, tenantLoginName, tenantType, beginTime, endTime, content);
        return getDataTable(list);
    }
}
