package com.jsh.erp.datasource.vo;

import java.math.BigDecimal;

public class MaterialExtendStock {
    private Long materialExtendId;
    private BigDecimal stock;

    public Long getMaterialExtendId() { return materialExtendId; }
    public void setMaterialExtendId(Long materialExtendId) { this.materialExtendId = materialExtendId; }
    public BigDecimal getStock() { return stock; }
    public void setStock(BigDecimal stock) { this.stock = stock; }
}
