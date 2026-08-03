package com.jsh.erp.datasource.mappers;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.jsh.erp.datasource.entities.Log;
import com.jsh.erp.datasource.entities.LogExample;
import com.jsh.erp.datasource.vo.LogVo4List;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface LogMapperEx {

    List<LogVo4List> selectByConditionLog(
            @Param("operation") String operation,
            @Param("userInfo") String userInfo,
            @Param("clientIp") String clientIp,
            @Param("tenantLoginName") String tenantLoginName,
            @Param("tenantType") String tenantType,
            @Param("beginTime") String beginTime,
            @Param("endTime") String endTime,
            @Param("content") String content);

    @InterceptorIgnore(tenantLine = "true")
    int insertLogWithUserId(Log log);

    @InterceptorIgnore(tenantLine = "true")
    int deleteLogsBefore(@Param("cutoff") Date cutoff);
}
