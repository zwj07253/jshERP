package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.SerialNumber;
import com.jsh.erp.datasource.entities.SerialNumberEx;
import com.jsh.erp.datasource.mappers.SequenceMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Description
 *
 * @Author: jishenghua
 * @Date: 2021/3/16 16:33
 */
@Service
public class SequenceService {
    private Logger logger = LoggerFactory.getLogger(SequenceService.class);

    @Resource
    private SequenceMapperEx sequenceMapperEx;

    private static final ZoneId BILL_ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter BILL_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int DAILY_NUMBER_MAX = 999999;

    public SerialNumber getSequence(long id)throws Exception {
        return null;
    }

    public List<SerialNumberEx> select(String name, Integer offset, Integer rows)throws Exception {
        return null;
    }

    public Long countSequence(String name)throws Exception {
        return null;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertSequence(JSONObject obj, HttpServletRequest request)throws Exception {
        return 0;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateSequence(JSONObject obj, HttpServletRequest request) throws Exception{
        return 0;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteSequence(Long id, HttpServletRequest request)throws Exception {
        return 0;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteSequence(String ids, HttpServletRequest request)throws Exception {
        return 0;
    }

    public int checkIsNameExist(Long id, String serialNumber)throws Exception {
        return 0;
    }

    /**
     * 获取唯一单据编号
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public String buildNumber(String prefixNo, Long tenantId) throws Exception {
        if (prefixNo == null || !prefixNo.matches("[A-Z0-9]{1,10}")) {
            throw new BusinessRunTimeException(
                    ExceptionConstants.SEQUENCE_ONLY_FAILED_CODE,
                    ExceptionConstants.SEQUENCE_ONLY_FAILED_MSG);
        }
        long safeTenantId = tenantId == null ? 0L : tenantId;
        String billDate = LocalDate.now(BILL_ZONE_ID).format(BILL_DATE_FORMAT);
        String seqName = "bill_" + safeTenantId + "_" + prefixNo + "_" + billDate;
        try {
            // PostgreSQL 在单条语句中完成插入或递增并返回结果，可保证并发下不重复。
            Long number = sequenceMapperEx.nextDailyNumber(seqName);
            if (number == null || number > DAILY_NUMBER_MAX) {
                throw new BusinessRunTimeException(
                        ExceptionConstants.SEQUENCE_DAILY_LIMIT_CODE,
                        ExceptionConstants.SEQUENCE_DAILY_LIMIT_MSG);
            }
            if (number < 1) {
                throw new BusinessRunTimeException(
                        ExceptionConstants.SEQUENCE_ONLY_FAILED_CODE,
                        ExceptionConstants.SEQUENCE_ONLY_FAILED_MSG);
            }
            return prefixNo + billDate + String.format("%06d", number);
        } catch (BusinessRunTimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("生成单据编号失败，prefixNo={}", prefixNo, e);
            throw new BusinessRunTimeException(ExceptionConstants.SEQUENCE_ONLY_BREAK_CODE, ExceptionConstants.SEQUENCE_ONLY_BREAK_MSG);
        }
    }
}
