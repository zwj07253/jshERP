package com.jsh.erp.datasource.vo;

import java.util.Objects;

public final class StockWarningKey {
    private final Long materialId;
    private final Long depotId;

    public StockWarningKey(Long materialId, Long depotId) {
        this.materialId = materialId;
        this.depotId = depotId;
    }

    public Long getMaterialId() { return materialId; }
    public Long getDepotId() { return depotId; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof StockWarningKey)) return false;
        StockWarningKey that = (StockWarningKey) object;
        return Objects.equals(materialId, that.materialId)
                && Objects.equals(depotId, that.depotId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(materialId, depotId);
    }
}
