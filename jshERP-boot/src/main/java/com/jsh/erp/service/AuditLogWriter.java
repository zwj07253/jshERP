package com.jsh.erp.service;

import com.jsh.erp.datasource.entities.Log;
import com.jsh.erp.datasource.mappers.LogMapperEx;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

@Service
public class AuditLogWriter {
    @Resource
    private LogMapperEx logMapperEx;

    @Transactional(value = "transactionManager", propagation = Propagation.REQUIRES_NEW,
            rollbackFor = Exception.class)
    public void write(Long userId, Long tenantId, String operation, String content,
                      String clientIp, byte status, Date createTime) {
        Log log = new Log();
        log.setUserId(userId);
        log.setTenantId(tenantId);
        log.setOperation(operation);
        log.setContent(content);
        log.setClientIp(clientIp);
        log.setStatus(status);
        log.setCreateTime(createTime);
        logMapperEx.insertLogWithUserId(log);
    }
}
