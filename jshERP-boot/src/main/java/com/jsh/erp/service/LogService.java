package com.jsh.erp.service;

import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.datasource.entities.Log;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.LogMapper;
import com.jsh.erp.datasource.mappers.LogMapperEx;
import com.jsh.erp.datasource.vo.LogVo4List;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.PageUtils;
import com.jsh.erp.utils.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class LogService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogService.class);
    private static final String LOG_URL = "/system/log";
    private static final int LOG_PERMISSION_CODE = 10700001;
    private static final String LOG_PERMISSION_MSG = "抱歉，您没有日志管理的查看权限";
    private static final int OPERATION_MAX_LENGTH = 500;
    private static final int CONTENT_MAX_LENGTH = 5000;
    private static final int IP_MAX_LENGTH = 200;
    private static final byte STATUS_SUCCESS = 0;
    private static final byte STATUS_FAILURE = 1;

    @Resource
    private LogMapper logMapper;
    @Resource
    private LogMapperEx logMapperEx;
    @Resource
    private UserService userService;
    @Resource
    private AuditLogWriter auditLogWriter;

    @Value("${jsh.log.retention-days:180}")
    private int retentionDays;

    public void checkReadPermission() throws Exception {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || !userService.hasFunctionPermission(currentUser.getId(), LOG_URL)) {
            throw new BusinessRunTimeException(LOG_PERMISSION_CODE, LOG_PERMISSION_MSG);
        }
    }

    public Log getLog(long id) throws Exception {
        try {
            return logMapper.selectByPrimaryKey(id);
        } catch (Exception e) {
            JshException.readFail(LOGGER, e);
            return null;
        }
    }

    public List<LogVo4List> select(String operation, String userInfo, String clientIp,
                                   String tenantLoginName, String tenantType,
                                   String beginTime, String endTime, String content) throws Exception {
        try {
            beginTime = Tools.parseDayToTime(beginTime, BusinessConstants.DAY_FIRST_TIME);
            endTime = Tools.parseDayToTime(endTime, BusinessConstants.DAY_LAST_TIME);
            PageUtils.startPage();
            List<LogVo4List> list = logMapperEx.selectByConditionLog(operation, userInfo, clientIp,
                    tenantLoginName, tenantType, beginTime, endTime, content);
            if (list != null) {
                for (LogVo4List log : list) {
                    log.setCreateTimeStr(Tools.getCenternTime(log.getCreateTime()));
                }
            }
            return list;
        } catch (Exception e) {
            JshException.readFail(LOGGER, e);
            return null;
        }
    }

    /** Audit failures must never roll back the business operation being audited. */
    public void insertLog(String moduleName, String content, HttpServletRequest request) {
        try {
            User user = getRequestUser(request);
            if (user != null && user.getId() != null) {
                writeSuccess(user.getId(), user.getTenantId(), moduleName, content, request);
            }
        } catch (Exception e) {
            LOGGER.error("Unable to write success audit log", e);
        }
    }

    public void insertLogWithUserId(Long userId, Long tenantId, String moduleName,
                                    String content, HttpServletRequest request) {
        if (userId == null) {
            return;
        }
        try {
            writeSuccess(userId, tenantId, moduleName, content, request);
        } catch (Exception e) {
            LOGGER.error("Unable to write login audit log for user {}", userId, e);
        }
    }

    public void insertFailureLog(Exception exception, HttpServletRequest request) {
        try {
            User user = getRequestUser(request);
            if (user == null || user.getId() == null) {
                return;
            }
            String method = request == null ? "UNKNOWN" : request.getMethod();
            String uri = request == null ? "unknown" : request.getRequestURI();
            String message = exception == null ? "unknown" : exception.getMessage();
            String content = method + " " + uri + " - "
                    + (exception == null ? "Exception" : exception.getClass().getSimpleName())
                    + ": " + (message == null ? "" : message);
            writeNow(user.getId(), user.getTenantId(), "请求失败", content, request, STATUS_FAILURE);
        } catch (Exception e) {
            LOGGER.error("Unable to write failure audit log", e);
        }
    }

    @Scheduled(cron = "${jsh.log.cleanup-cron:0 30 2 * * ?}")
    public void cleanupExpiredLogs() {
        int days = Math.max(retentionDays, 1);
        Date cutoff = Date.from(Instant.now().minus(days, ChronoUnit.DAYS));
        try {
            int deleted = logMapperEx.deleteLogsBefore(cutoff);
            if (deleted > 0) {
                LOGGER.info("Cleaned {} audit logs older than {} days", deleted, days);
            }
        } catch (Exception e) {
            LOGGER.error("Unable to clean expired audit logs", e);
        }
    }

    private void writeSuccess(Long userId, Long tenantId, String moduleName, String content,
                              HttpServletRequest request) {
        Long trustedTenantId = tenantId == null ? 0L : tenantId;
        String operation = limit(moduleName, OPERATION_MAX_LENGTH, "未知操作");
        String detail = limit(content, CONTENT_MAX_LENGTH, "");
        String clientIp = limit(trustedRemoteAddress(request), IP_MAX_LENGTH, "unknown");
        Date createTime = new Date();
        Runnable task = () -> {
            try {
                auditLogWriter.write(userId, trustedTenantId, operation, detail,
                        clientIp, STATUS_SUCCESS, createTime);
            } catch (Exception e) {
                LOGGER.error("Unable to persist committed success audit log", e);
            }
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    private void writeNow(Long userId, Long tenantId, String moduleName, String content,
                          HttpServletRequest request, byte status) {
        auditLogWriter.write(userId, tenantId == null ? 0L : tenantId,
                limit(moduleName, OPERATION_MAX_LENGTH, "未知操作"),
                limit(content, CONTENT_MAX_LENGTH, ""),
                limit(trustedRemoteAddress(request), IP_MAX_LENGTH, "unknown"),
                status, new Date());
    }

    private String trustedRemoteAddress(HttpServletRequest request) {
        if (request == null || request.getRemoteAddr() == null) {
            return "unknown";
        }
        return request.getRemoteAddr();
    }

    private User getRequestUser(HttpServletRequest request) throws Exception {
        if (request == null) {
            return null;
        }
        Long userId = userService.getUserId(request);
        return userId == null ? null : userService.getUser(userId);
    }

    private String limit(String value, int maxLength, String fallback) {
        String normalized = value == null || value.trim().isEmpty() ? fallback : value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
