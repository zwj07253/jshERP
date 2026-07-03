package com.jsh.erp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.gitee.starblues.integration.application.PluginApplication;
import com.gitee.starblues.integration.operator.PluginOperator;
import com.gitee.starblues.integration.operator.upload.UploadParam;
import com.gitee.starblues.core.PluginInfo;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.service.UserService;
import com.jsh.erp.utils.BaseResponseInfo;
import com.jsh.erp.utils.ComputerInfo;
import com.jsh.erp.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/plugin")
@Tag(name = "插件管理")
public class PluginController {
    private Logger logger = LoggerFactory.getLogger(PluginController.class);

    @Resource
    private UserService userService;

    private final PluginOperator pluginOperator;

    @Autowired
    public PluginController(PluginApplication pluginApplication) {
        this.pluginOperator = pluginApplication.getPluginOperator();
    }

    @GetMapping(value = "/list")
    @Operation(summary = "获取插件信息")
    public BaseResponseInfo getPluginInfo(@RequestParam(value = "name", required = false) String name,
                                          @RequestParam("currentPage") Integer currentPage,
                                          @RequestParam("pageSize") Integer pageSize,
                                          HttpServletRequest request) throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<>();
        try {
            List<PluginInfo> resList = new ArrayList<>();
            User userInfo = userService.getCurrentUser();
            if (BusinessConstants.DEFAULT_MANAGER.equals(userInfo.getLoginName())) {
                List<PluginInfo> list = pluginOperator.getPluginInfo();
                if (StringUtil.isEmpty(name)) {
                    resList = list;
                } else {
                    for (PluginInfo pi : list) {
                        String desc = pi.getPluginDescriptor().getDescription();
                        if (desc != null && desc.contains(name)) {
                            resList.add(pi);
                        }
                    }
                }
            }
            map.put("rows", resList);
            map.put("total", resList.size());
            res.code = 200;
            res.data = map;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }

    @PostMapping("/stop/{id}")
    @Operation(summary = "根据插件id停止插件")
    public BaseResponseInfo stop(@PathVariable("id") String id) {
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<>();
        try {
            User userInfo = userService.getCurrentUser();
            if (BusinessConstants.DEFAULT_MANAGER.equals(userInfo.getLoginName())) {
                pluginOperator.stop(id);
                map.put("message", "plugin '" + id + "' stop success");
            } else {
                map.put("message", "power is limit");
            }
            res.code = 200;
            res.data = map;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            map.put("message", "plugin '" + id + "' stop failure. " + e.getMessage());
            res.code = 500;
            res.data = map;
        }
        return res;
    }

    @PostMapping("/start/{id}")
    @Operation(summary = "根据插件id启动插件")
    public BaseResponseInfo start(@PathVariable("id") String id) {
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<>();
        try {
            User userInfo = userService.getCurrentUser();
            if (BusinessConstants.DEFAULT_MANAGER.equals(userInfo.getLoginName())) {
                pluginOperator.start(id);
                map.put("message", "plugin '" + id + "' start success");
            } else {
                map.put("message", "power is limit");
            }
            res.code = 200;
            res.data = map;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            map.put("message", "plugin '" + id + "' start failure. " + e.getMessage());
            res.code = 500;
            res.data = map;
        }
        return res;
    }

    @PostMapping("/uninstall/{id}")
    @Operation(summary = "根据插件id卸载插件")
    public BaseResponseInfo uninstall(@PathVariable("id") String id) {
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<>();
        try {
            User userInfo = userService.getCurrentUser();
            if (BusinessConstants.DEFAULT_MANAGER.equals(userInfo.getLoginName())) {
                pluginOperator.uninstall(id, true, true);
                map.put("message", "plugin '" + id + "' uninstall success");
            } else {
                map.put("message", "power is limit");
            }
            res.code = 200;
            res.data = map;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            map.put("message", "plugin '" + id + "' uninstall failure. " + e.getMessage());
            res.code = 500;
            res.data = map;
        }
        return res;
    }

    @PostMapping("/installByPath")
    @Operation(summary = "根据插件路径安装插件")
    public String install(@RequestParam("path") String path) {
        try {
            User userInfo = userService.getCurrentUser();
            if (BusinessConstants.DEFAULT_MANAGER.equals(userInfo.getLoginName())) {
                pluginOperator.install(Paths.get(path), true);
                return "installByPath success";
            } else {
                return "installByPath failure: power is limit";
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return "installByPath failure : " + e.getMessage();
        }
    }

    @PostMapping("/uploadInstallPluginJar")
    @Operation(summary = "上传并安装插件")
    public BaseResponseInfo install(MultipartFile file, HttpServletRequest request, HttpServletResponse response) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            User userInfo = userService.getCurrentUser();
            if (BusinessConstants.DEFAULT_MANAGER.equals(userInfo.getLoginName())) {
                UploadParam uploadParam = UploadParam.byMultipartFile(file).setStartPlugin(true);
                pluginOperator.uploadPlugin(uploadParam);
                res.code = 200;
                res.data = "导入成功";
            } else {
                res.code = 500;
                res.data = "抱歉，无操作权限！";
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "导入失败";
        }
        return res;
    }

    @PostMapping("/back/{pluginId}")
    @Operation(summary = "备份插件")
    public String backupPlugin(@PathVariable("pluginId") String pluginId) {
        try {
            User userInfo = userService.getCurrentUser();
            if (BusinessConstants.DEFAULT_MANAGER.equals(userInfo.getLoginName())) {
                Path backupPath = pluginOperator.backupPlugin(pluginId, "backupPlugin");
                return "backupPlugin success: " + backupPath;
            } else {
                return "backupPlugin failure: power is limit";
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return "backupPlugin failure : " + e.getMessage();
        }
    }

    @GetMapping("/getMacWithSecret")
    @Operation(summary = "获取加密后的mac")
    public BaseResponseInfo getMacWithSecret() {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            String mac = ComputerInfo.getMacAddress();
            res.code = 200;
            res.data = DigestUtils.md5DigestAsHex(mac.getBytes());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }

    @GetMapping("/checkByPluginId")
    @Operation(summary = "根据插件标识判断是否存在")
    public BaseResponseInfo checkByTag(@RequestParam("pluginIds") String pluginIds) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            boolean data = false;
            if (StringUtil.isNotEmpty(pluginIds)) {
                String[] pluginIdList = pluginIds.split(",");
                List<PluginInfo> list = pluginOperator.getPluginInfo();
                for (PluginInfo pi : list) {
                    String info = pi.getPluginDescriptor().getPluginId();
                    for (String pluginId : pluginIdList) {
                        if (pluginId.equals(info)) {
                            data = true;
                        }
                    }
                }
            }
            res.code = 200;
            res.data = data;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }
}
