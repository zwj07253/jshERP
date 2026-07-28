package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.StockWarningStatus;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

public interface StockWarningStatusMapper {
    int insertIfAbsent(@Param("tenantId") Long tenantId,
                       @Param("materialId") Long materialId,
                       @Param("depotId") Long depotId,
                       @Param("warningType") String warningType,
                       @Param("active") boolean active,
                       @Param("currentStock") BigDecimal currentStock);

    StockWarningStatus selectForUpdate(@Param("tenantId") Long tenantId,
                                       @Param("materialId") Long materialId,
                                       @Param("depotId") Long depotId,
                                       @Param("warningType") String warningType);

    int updateState(@Param("id") Long id,
                    @Param("active") boolean active,
                    @Param("currentStock") BigDecimal currentStock,
                    @Param("notified") boolean notified);
}
