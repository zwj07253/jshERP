package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.vo.DepotHeadVo4StatementAccount;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StatementAccountMapper {

    List<DepotHeadVo4StatementAccount> selectStatementAccount(
            @Param("beginTime") String beginTime,
            @Param("endTime") String endTime,
            @Param("organId") Integer organId,
            @Param("organArray") String[] organArray,
            @Param("hasDebt") Integer hasDebt,
            @Param("supplierType") String supplierType,
            @Param("type") String type,
            @Param("subType") String subType,
            @Param("typeBack") String typeBack,
            @Param("subTypeBack") String subTypeBack,
            @Param("billType") String billType,
            @Param("column") String column,
            @Param("order") String order,
            @Param("offset") Integer offset,
            @Param("rows") Integer rows);

    int countStatementAccount(
            @Param("beginTime") String beginTime,
            @Param("endTime") String endTime,
            @Param("organId") Integer organId,
            @Param("organArray") String[] organArray,
            @Param("hasDebt") Integer hasDebt,
            @Param("supplierType") String supplierType,
            @Param("type") String type,
            @Param("subType") String subType,
            @Param("typeBack") String typeBack,
            @Param("subTypeBack") String subTypeBack,
            @Param("billType") String billType);

    DepotHeadVo4StatementAccount sumStatementAccount(
            @Param("beginTime") String beginTime,
            @Param("endTime") String endTime,
            @Param("organId") Integer organId,
            @Param("organArray") String[] organArray,
            @Param("hasDebt") Integer hasDebt,
            @Param("supplierType") String supplierType,
            @Param("type") String type,
            @Param("subType") String subType,
            @Param("typeBack") String typeBack,
            @Param("subTypeBack") String subTypeBack,
            @Param("billType") String billType);
}
