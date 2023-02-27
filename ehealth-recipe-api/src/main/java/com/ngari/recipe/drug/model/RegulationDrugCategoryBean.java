package com.ngari.recipe.drug.model;

import ctd.schema.annotation.ItemProperty;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 互联网医院药品目录
 */
public class RegulationDrugCategoryBean implements java.io.Serializable {

    private static final long serialVersionUID = -3090605406715918633L;

    @NotNull
    private String unitID;

    @NotNull
    private String organID;

    @NotNull
    private String organName;

    @NotNull
    @ItemProperty(alias = "监管平台药品ID")
    private String platDrugCode;

    @NotNull
    @ItemProperty(alias = "医院药品代码")
    private String hospDrugCode;

    @ItemProperty(alias = "监管平台药品通用名")
    private String platDrugName;

    @NotNull
    @ItemProperty(alias = "医院药品通用名")
    private String hospDrugName;

    @ItemProperty(alias = "医院药品商品名")
    private String hospTradeName;

    @ItemProperty(alias = "医院药品别名")
    private String hospDrugAlias;

    @ItemProperty(alias = "医院药品规格")
    private String hospDrugSpec;

    @NotNull
    @ItemProperty(alias = "医院药品包装规格")
    private String hospDrugPacking;

    @NotNull
    @ItemProperty(alias = "医院药品产地名称")
    private String hospDrugManuf;

    @NotNull
    @ItemProperty(alias = "医院有效标志")
    private String useFlag;

    @NotNull
    @ItemProperty(alias = "药品分类代码")
    private String drugClass;

    @ItemProperty(alias = "生产企业")
    private String productionEnterprise;

    @ItemProperty(alias = "配送企业")
    private String distributionEnterprise;

    @ItemProperty(alias = "最后更新时间")
    private Date updateTime;

    @ItemProperty(alias = "数据库记录创建时间")
    private Date createTime;

    @ItemProperty(alias = "数据库记录最后修改时间")
    private Date lastModify;

    @ItemProperty(alias = "指标结果 null 没执行  1 通过 2不通过")
    private String checkRes;

    @ItemProperty(alias = "是否警告")
    private String warn;

    @ItemProperty(alias = "是否违规")
    private String error;

    @ItemProperty(alias = "医院药品单价")
    private BigDecimal drugPrice;

    @ItemProperty(alias = "项目标准代码")
    private String medicalDrugCode;

    @ItemProperty(alias = "批准文号")
    private String licenseNumber;

    @ItemProperty(alias = "剂型代码")
    private String drugFormCode;

    @ItemProperty(alias = "剂型名称")
    private String drugForm;

    @ItemProperty(alias = "院内制剂标志：0.非自制药品；1.自制药品")
    private String hospitalPreparation;

    @ItemProperty(alias = "是否基药 1：国基药；2：上海增补基药；0：非前述两种")
    private String baseDrug;

    @ItemProperty(alias = "抗生素标识 1：抗生素；0：非抗生素")
    private String kssFlag;

    @ItemProperty(alias = "毒麻精放标识1：是；0：否")
    private String dmjfFlag;

    @ItemProperty(alias = "备注说明")
    private String noteDesc;

    @ItemProperty(alias = "1.正常, 2.撤销")
    private String modifyFlag;

    public String getUnitID() {
        return unitID;
    }

    public void setUnitID(String unitID) {
        this.unitID = unitID;
    }

    public String getOrganID() {
        return organID;
    }

    public void setOrganID(String organID) {
        this.organID = organID;
    }

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public String getPlatDrugCode() {
        return platDrugCode;
    }

    public void setPlatDrugCode(String platDrugCode) {
        this.platDrugCode = platDrugCode;
    }

    public String getHospDrugCode() {
        return hospDrugCode;
    }

    public void setHospDrugCode(String hospDrugCode) {
        this.hospDrugCode = hospDrugCode;
    }

    public String getPlatDrugName() {
        return platDrugName;
    }

    public void setPlatDrugName(String platDrugName) {
        this.platDrugName = platDrugName;
    }

    public String getHospDrugName() {
        return hospDrugName;
    }

    public void setHospDrugName(String hospDrugName) {
        this.hospDrugName = hospDrugName;
    }

    public String getHospTradeName() {
        return hospTradeName;
    }

    public void setHospTradeName(String hospTradeName) {
        this.hospTradeName = hospTradeName;
    }

    public String getHospDrugAlias() {
        return hospDrugAlias;
    }

    public void setHospDrugAlias(String hospDrugAlias) {
        this.hospDrugAlias = hospDrugAlias;
    }

    public String getHospDrugSpec() {
        return hospDrugSpec;
    }

    public void setHospDrugSpec(String hospDrugSpec) {
        this.hospDrugSpec = hospDrugSpec;
    }

    public String getHospDrugPacking() {
        return hospDrugPacking;
    }

    public void setHospDrugPacking(String hospDrugPacking) {
        this.hospDrugPacking = hospDrugPacking;
    }

    public String getHospDrugManuf() {
        return hospDrugManuf;
    }

    public void setHospDrugManuf(String hospDrugManuf) {
        this.hospDrugManuf = hospDrugManuf;
    }

    public String getUseFlag() {
        return useFlag;
    }

    public void setUseFlag(String useFlag) {
        this.useFlag = useFlag;
    }

    public String getDrugClass() {
        return drugClass;
    }

    public void setDrugClass(String drugClass) {
        this.drugClass = drugClass;
    }

    public String getProductionEnterprise() {
        return productionEnterprise;
    }

    public void setProductionEnterprise(String productionEnterprise) {
        this.productionEnterprise = productionEnterprise;
    }

    public String getDistributionEnterprise() {
        return distributionEnterprise;
    }

    public void setDistributionEnterprise(String distributionEnterprise) {
        this.distributionEnterprise = distributionEnterprise;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    public String getCheckRes() {
        return checkRes;
    }

    public void setCheckRes(String checkRes) {
        this.checkRes = checkRes;
    }

    public String getWarn() {
        return warn;
    }

    public void setWarn(String warn) {
        this.warn = warn;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public BigDecimal getDrugPrice() {
        return drugPrice;
    }

    public void setDrugPrice(BigDecimal drugPrice) {
        this.drugPrice = drugPrice;
    }

    public String getMedicalDrugCode() {
        return medicalDrugCode;
    }

    public void setMedicalDrugCode(String medicalDrugCode) {
        this.medicalDrugCode = medicalDrugCode;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getDrugFormCode() {
        return drugFormCode;
    }

    public void setDrugFormCode(String drugFormCode) {
        this.drugFormCode = drugFormCode;
    }

    public String getDrugForm() {
        return drugForm;
    }

    public void setDrugForm(String drugForm) {
        this.drugForm = drugForm;
    }

    public String getHospitalPreparation() {
        return hospitalPreparation;
    }

    public void setHospitalPreparation(String hospitalPreparation) {
        this.hospitalPreparation = hospitalPreparation;
    }

    public String getBaseDrug() {
        return baseDrug;
    }

    public void setBaseDrug(String baseDrug) {
        this.baseDrug = baseDrug;
    }

    public String getKssFlag() {
        return kssFlag;
    }

    public void setKssFlag(String kssFlag) {
        this.kssFlag = kssFlag;
    }

    public String getDmjfFlag() {
        return dmjfFlag;
    }

    public void setDmjfFlag(String dmjfFlag) {
        this.dmjfFlag = dmjfFlag;
    }

    public String getNoteDesc() {
        return noteDesc;
    }

    public void setNoteDesc(String noteDesc) {
        this.noteDesc = noteDesc;
    }

    public String getModifyFlag() {
        return modifyFlag;
    }

    public void setModifyFlag(String modifyFlag) {
        this.modifyFlag = modifyFlag;
    }
}
