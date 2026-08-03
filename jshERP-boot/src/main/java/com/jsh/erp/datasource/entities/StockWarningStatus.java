package com.jsh.erp.datasource.entities;

import java.math.BigDecimal;
import java.util.Date;

public class StockWarningStatus {
    private Long id;
    private Long tenantId;
    private Long materialId;
    private Long depotId;
    private String warningType;
    private Boolean active;
    private BigDecimal currentStock;
    private Date notifyTime;
    private Date createTime;
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public Long getDepotId() { return depotId; }
    public void setDepotId(Long depotId) { this.depotId = depotId; }
    public String getWarningType() { return warningType; }
    public void setWarningType(String warningType) { this.warningType = warningType; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public BigDecimal getCurrentStock() { return currentStock; }
    public void setCurrentStock(BigDecimal currentStock) { this.currentStock = currentStock; }
    public Date getNotifyTime() { return notifyTime; }
    public void setNotifyTime(Date notifyTime) { this.notifyTime = notifyTime; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
