package com.jsh.erp.datasource.vo;

import java.math.BigDecimal;

public class DepotItemVo4InOutStock {

    private Long id;
    private String barCode;
    private String materialName;
    private String materialModel;
    private String materialStandard;
    private String materialColor;
    private String materialMfrs;
    private String materialBrand;
    private String otherField1;
    private String otherField2;
    private String otherField3;
    private String materialUnit;
    private Long unitId;
    private String unitName;
    private BigDecimal unitRatio;
    private String otherUnit;
    private BigDecimal unitPrice;
    private BigDecimal prevSum;
    private BigDecimal inSum;
    private BigDecimal outSum;
    private BigDecimal thisSum;
    private BigDecimal thisAllPrice;
    private String imgName;
    private BigDecimal totalStock;
    private BigDecimal totalCountMoney;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBarCode() { return barCode; }
    public void setBarCode(String barCode) { this.barCode = barCode; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getMaterialModel() { return materialModel; }
    public void setMaterialModel(String materialModel) { this.materialModel = materialModel; }
    public String getMaterialStandard() { return materialStandard; }
    public void setMaterialStandard(String materialStandard) { this.materialStandard = materialStandard; }
    public String getMaterialColor() { return materialColor; }
    public void setMaterialColor(String materialColor) { this.materialColor = materialColor; }
    public String getMaterialMfrs() { return materialMfrs; }
    public void setMaterialMfrs(String materialMfrs) { this.materialMfrs = materialMfrs; }
    public String getMaterialBrand() { return materialBrand; }
    public void setMaterialBrand(String materialBrand) { this.materialBrand = materialBrand; }
    public String getOtherField1() { return otherField1; }
    public void setOtherField1(String otherField1) { this.otherField1 = otherField1; }
    public String getOtherField2() { return otherField2; }
    public void setOtherField2(String otherField2) { this.otherField2 = otherField2; }
    public String getOtherField3() { return otherField3; }
    public void setOtherField3(String otherField3) { this.otherField3 = otherField3; }
    public String getMaterialUnit() { return materialUnit; }
    public void setMaterialUnit(String materialUnit) { this.materialUnit = materialUnit; }
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public BigDecimal getUnitRatio() { return unitRatio; }
    public void setUnitRatio(BigDecimal unitRatio) { this.unitRatio = unitRatio; }
    public String getOtherUnit() { return otherUnit; }
    public void setOtherUnit(String otherUnit) { this.otherUnit = otherUnit; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getPrevSum() { return prevSum; }
    public void setPrevSum(BigDecimal prevSum) { this.prevSum = prevSum; }
    public BigDecimal getInSum() { return inSum; }
    public void setInSum(BigDecimal inSum) { this.inSum = inSum; }
    public BigDecimal getOutSum() { return outSum; }
    public void setOutSum(BigDecimal outSum) { this.outSum = outSum; }
    public BigDecimal getThisSum() { return thisSum; }
    public void setThisSum(BigDecimal thisSum) { this.thisSum = thisSum; }
    public BigDecimal getThisAllPrice() { return thisAllPrice; }
    public void setThisAllPrice(BigDecimal thisAllPrice) { this.thisAllPrice = thisAllPrice; }
    public String getImgName() { return imgName; }
    public void setImgName(String imgName) { this.imgName = imgName; }
    public BigDecimal getTotalStock() { return totalStock; }
    public void setTotalStock(BigDecimal totalStock) { this.totalStock = totalStock; }
    public BigDecimal getTotalCountMoney() { return totalCountMoney; }
    public void setTotalCountMoney(BigDecimal totalCountMoney) { this.totalCountMoney = totalCountMoney; }
}
