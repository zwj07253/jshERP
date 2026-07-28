package com.jsh.erp.datasource.mappers;

import org.apache.ibatis.annotations.Param;

public interface SequenceMapperEx {

    /**
     * 按序列名称原子递增并返回新值。
     */
    Long nextDailyNumber(@Param("seqName") String seqName);
}
