package com.jsh.erp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.gitee.starblues.integration.application.PluginApplication;
import com.gitee.starblues.integration.operator.PluginOperator;
import com.gitee.starblues.integration.operator.upload.UploadParam;
import com.gitee.starblues.core.PluginInfo;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.service.LogService;
import com.jsh.erp.service.SystemConfigService;
import com.jsh.erp.service.UserService;
import com.jsh.erp.utils.BaseResponseInfo;
import com.jsh.erp.utils.ComputerInfo;
import com.jsh.erp.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@RestController
@RequestMapping("/plugin")
@Tag(name = "插件管理")
public class PluginController {
    private Logger logger = LoggerFactory.getLogger(PluginController.class);

    private static final long MAX_PLUGIN_SIZE = 50 * 1024 * 1024; // 50MB

    @Resource
    private UserService userService;
    @Resource
    private LogService logService;
    @Resource
    private SystemConfigService systemConfigService;

    @Value("${pluginPath:plugins}")
    private String pluginPath;

    private final PluginOperator pluginOperator;

    private final ConcurrentHashMap<String, ReentrantLock> pluginLocks = new ConcurrentHashMap<>();

    @Autowired
    public PluginController(PluginApplication pluginApplication) {
        this.pluginOperator = pluginApplication.getPluginOperator();
    }

    private ReentrantLock getPluginLock(String pluginId) {
        return pluginLocks.computeIfAbsent(pluginId, k -> new ReentrantLock());
    }

    private String computeSha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private boolean isAdmin(User user) {
        return user != null && BusinessConstants.DEFAULT_MANAGER.equals(user.getLoginName());
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
            if (isAdmin(userInfo)) {
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
    public BaseResponseInfo stop(@PathVariable("id") String id, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<>();
        ReentrantLock lock = getPluginLock(id);
        try {
            User userInfo = userService.getCurrentUser();
            if (!isAdmin(userInfo)) {
                res.code = 403;
                res.data = "权限不足";
                return res;
            }
            if (!lock.tryLock()) {
                res.code = ExceptionConstants.PLUGIN_OPERATION_CONFLICT_CODE;
                res.data = ExceptionConstants.PLUGIN_OPERATION_CONFLICT_MSG;
                return res;
            }
            try {
                pluginOperator.stop(id);
                map.put("message", "plugin '" + id + "' stop success");
                logService.insertLog("插件", "停止插件: " + id, request);
            } finally {
                lock.unlock();
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
    public BaseResponseInfo start(@PathVariable("id") String id, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<>();
        ReentrantLock lock = getPluginLock(id);
        try {
            User userInfo = userService.getCurrentUser();
            if (!isAdmin(userInfo)) {
                res.code = 403;
                res.data = "权限不足";
                return res;
            }
            if (!lock.tryLock()) {
                res.code = ExceptionConstants.PLUGIN_OPERATION_CONFLICT_CODE;
                res.data = ExceptionConstants.PLUGIN_OPERATION_CONFLICT_MSG;
                return res;
            }
            try {
                pluginOperator.start(id);
                map.put("message", "plugin '" + id + "' start success");
                logService.insertLog("插件", "启动插件: " + id, request);
            } finally {
                lock.unlock();
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
    public BaseResponseInfo uninstall(@PathVariable("id") String id, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<>();
        ReentrantLock lock = getPluginLock(id);
        try {
            User userInfo = userService.getCurrentUser();
            if (!isAdmin(userInfo)) {
                res.code = 403;
                res.data = "权限不足";
                return res;
            }
            // 卸载前依赖检查：workflow 插件被系统配置启用时拒绝卸载
            if ("workflow".equals(id)) {
                String depCheck = checkWorkflowDependency();
                if (depCheck != null) {
                    res.code = ExceptionConstants.PLUGIN_IN_USE_CODE;
                    res.data = String.format(ExceptionConstants.PLUGIN_IN_USE_MSG, depCheck);
                    return res;
                }
            }
            if (!lock.tryLock()) {
                res.code = ExceptionConstants.PLUGIN_OPERATION_CONFLICT_CODE;
                res.data = ExceptionConstants.PLUGIN_OPERATION_CONFLICT_MSG;
                return res;
            }
            try {
                // 默认保留配置和备份，避免误卸载无法恢复
                pluginOperator.uninstall(id, false, false);
                map.put("message", "plugin '" + id + "' uninstall success");
                logService.insertLog("插件", "卸载插件: " + id, request);
            } finally {
                lock.unlock();
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

    private String checkWorkflowDependency() {
        try {
            List<com.jsh.erp.datasource.entities.SystemConfig> configs = systemConfigService.getSystemConfig();
            for (com.jsh.erp.datasource.entities.SystemConfig config : configs) {
                if (config.getMultiLevelApprovalFlag() != null
                        && "1".equals(config.getMultiLevelApprovalFlag())) {
                    return "多级审核功能已启用";
                }
            }
        } catch (Exception e) {
            logger.warn("检查 workflow 依赖时出错", e);
        }
        return null;
    }

    @PostMapping("/installByPath")
    @Operation(summary = "根据插件路径安装插件")
    public String install(@RequestParam("path") String path, HttpServletRequest request) {
        try {
            User userInfo = userService.getCurrentUser();
            if (!isAdmin(userInfo)) {
                return "installByPath failure: 权限不足";
            }
            // 路径安全校验：只允许从插件目录安装
            Path allowedRoot = Paths.get(pluginPath).toAbsolutePath().normalize();
            Path targetPath = Paths.get(path).toAbsolutePath().normalize();
            if (!targetPath.startsWith(allowedRoot)) {
                return "installByPath failure: 非法路径，只允许从插件目录安装";
            }
            // 校验文件扩展名
            String fileName = targetPath.getFileName().toString();
            if (!fileName.toLowerCase().endsWith(".jar")) {
                return "installByPath failure: 只允许安装 .jar 文件";
            }
            pluginOperator.install(targetPath, true);
            logService.insertLog("插件", "从路径安装插件: " + path, request);
            return "installByPath success";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return "installByPath failure : " + e.getMessage();
        }
    }

    @PostMapping("/uploadInstallPluginJar")
    @Operation(summary = "上传并安装插件")
    public BaseResponseInfo install(MultipartFile file, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            User userInfo = userService.getCurrentUser();
            if (!isAdmin(userInfo)) {
                res.code = 403;
                res.data = "权限不足";
                return res;
            }
            // 第一层：文件非空检查
            if (file == null || file.isEmpty()) {
                res.code = ExceptionConstants.PLUGIN_FILE_INVALID_CODE;
                res.data = String.format(ExceptionConstants.PLUGIN_FILE_INVALID_MSG, "文件为空");
                return res;
            }
            // 第二层：文件大小限制
            if (file.getSize() > MAX_PLUGIN_SIZE) {
                res.code = ExceptionConstants.PLUGIN_FILE_INVALID_CODE;
                res.data = String.format(ExceptionConstants.PLUGIN_FILE_INVALID_MSG, "文件大小超过50MB限制");
                return res;
            }
            // 第三层：文件扩展名检查
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".jar")) {
                res.code = ExceptionConstants.PLUGIN_FILE_INVALID_CODE;
                res.data = String.format(ExceptionConstants.PLUGIN_FILE_INVALID_MSG, "只允许上传 .jar 文件");
                return res;
            }
            // 第四层：JAR 格式验证
            byte[] fileBytes = file.getBytes();
            if (!isValidJar(fileBytes)) {
                res.code = ExceptionConstants.PLUGIN_FILE_INVALID_CODE;
                res.data = String.format(ExceptionConstants.PLUGIN_FILE_INVALID_MSG, "无法读取JAR文件，格式可能已损坏");
                return res;
            }
            // 记录 SHA-256 用于审计
            String sha256 = computeSha256(fileBytes);
            logger.info("上传插件JAR: filename={}, size={}, sha256={}", originalFilename, file.getSize(), sha256);
            // 安装并启动
            UploadParam uploadParam = UploadParam.byMultipartFile(file).setStartPlugin(true);
            pluginOperator.uploadPlugin(uploadParam);
            logService.insertLog("插件", "上传安装插件: " + originalFilename + ", SHA-256: " + sha256, request);
            res.code = 200;
            res.data = "导入成功";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "导入失败: " + e.getMessage();
        }
        return res;
    }

    private boolean isValidJar(byte[] fileBytes) {
        try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(fileBytes);
             java.util.jar.JarInputStream jis = new java.util.jar.JarInputStream(bais)) {
            // 能正常读取 JAR manifest 即可
            jis.getManifest();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @PostMapping("/back/{pluginId}")
    @Operation(summary = "备份插件")
    public String backupPlugin(@PathVariable("pluginId") String pluginId, HttpServletRequest request) {
        try {
            User userInfo = userService.getCurrentUser();
            if (!isAdmin(userInfo)) {
                return "backupPlugin failure: 权限不足";
            }
            // 限制 pluginId 只允许安全字符
            if (!pluginId.matches("[a-zA-Z0-9._-]+")) {
                return "backupPlugin failure: 非法插件标识";
            }
            Path backupPath = pluginOperator.backupPlugin(pluginId, "backupPlugin");
            logService.insertLog("插件", "备份插件: " + pluginId + ", 路径: " + backupPath, request);
            return "backupPlugin success: " + backupPath;
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
            Map<String, Object> result = new LinkedHashMap<>();
            if (StringUtil.isNotEmpty(pluginIds)) {
                String[] pluginIdList = pluginIds.split(",");
                List<PluginInfo> list = pluginOperator.getPluginInfo();
                for (String pluginId : pluginIdList) {
                    String trimmedId = pluginId.trim();
                    Map<String, Object> status = new LinkedHashMap<>();
                    status.put("installed", false);
                    status.put("started", false);
                    for (PluginInfo pi : list) {
                        if (trimmedId.equals(pi.getPluginDescriptor().getPluginId())) {
                            status.put("installed", true);
                            status.put("started", "STARTED".equals(pi.getPluginState().toString()));
                            break;
                        }
                    }
                    result.put(trimmedId, status);
                }
            }
            res.code = 200;
            res.data = result;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }
}
