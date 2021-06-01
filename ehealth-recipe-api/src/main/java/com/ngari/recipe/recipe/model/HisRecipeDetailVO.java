package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author yinsheng
 * @date 2020\3\12 0012 18:23
 */
public class HisRecipeDetailVO implements Serializable{
    private static final long serialVersionUID = -7771767746462020320L;

    @ItemProperty(alias = "处方详情序号")
    private Integer hisrecipedetailID; // int(11) NOT NULL AUTO_INCREMENT,
    @ItemProperty(alias = "his处方序号")
    private Integer hisRecipeId; // int(11) NOT NULL COMMENT 'his处方序号',
    @ItemProperty(alias = "药品明细编码")
    private String recipeDeatilCode; // varchar(50) DEFAULT NULL COMMENT '药品明细编码',
    @ItemProperty(alias = "通用名")
    private String drugName; // varchar(50) DEFAULT NULL COMMENT '通用名',
    @ItemProperty(alias = "商品名")
    private String saleName;// varchar(50) DEFAULT NULL COMMENT '商品名',
    @ItemProperty(alias = "药品规格")
    private String drugSpec;// varchar(50) NOT NULL COMMENT '药品规格',
    @ItemProperty(alias = "批准文号")
    private String licenseNumber;// varchar(50) DEFAULT NULL COMMENT '批准文号',
    @ItemProperty(alias = "药品本位码")
    private String standardCode; // varchar(50) DEFAULT NULL COMMENT '药品本位码',
    @ItemProperty(alias = "药品生产厂家")
    private String producer; // varchar(50) DEFAULT NULL COMMENT '药品生产厂家',
    @ItemProperty(alias = "厂家编码")
    private String producerCode;// varchar(50) DEFAULT NULL COMMENT '厂家编码',
    @ItemProperty(alias = "开药总数")
    private BigDecimal useTotalDose; // decimal(10,3) DEFAULT NULL COMMENT '开药总数',
    @ItemProperty(alias = "包装单位")
    private String drugUnit; // varchar(10) DEFAULT NULL COMMENT '包装单位',
    @ItemProperty(alias = "每次使用剂量")
    private String useDose;// varchar(20) DEFAULT NULL COMMENT '每次使用剂量',
    @ItemProperty(alias = "每次使用剂量特殊用法")
    private String useDoseStr;
    @ItemProperty(alias = "包装数量")
    private Integer pack; // int(10) DEFAULT NULL COMMENT '包装数量',
    @ItemProperty(alias = "药品单价")
    private BigDecimal price;// decimal(10,2) DEFAULT NULL COMMENT '药品单价',
    @ItemProperty(alias = "药品总价")
    private BigDecimal totalPrice;// decimal(10,2) DEFAULT NULL COMMENT '药品总价',
    @ItemProperty(alias = "用药频率")
    private String usingRate;// varchar(20) DEFAULT NULL COMMENT '用药频率',
    @ItemProperty(alias = "用药方式")
    private String usePathways; //varchar(20) DEFAULT NULL COMMENT '用药方式',
    @ItemProperty(alias = "用药频率说明")
    private String usingRateText;// varchar(20) DEFAULT NULL COMMENT '用药频率说明',
    @ItemProperty(alias = "用药方式说明")
    private String usePathwaysText;// varchar(20) DEFAULT NULL COMMENT '用药方式说明',
    @ItemProperty(alias = "药品使用备注")
    private String memo; // varchar(255) DEFAULT NULL COMMENT '药品使用备注',
    @ItemProperty(alias = "说明")
    private String remark; // varchar(255) DEFAULT NULL COMMENT '说明',
    @ItemProperty(alias = "状态")
    private Integer status; // tinyint(1) DEFAULT NULL COMMENT '0 不可在互联网流转 1 可以流转',
    @ItemProperty(alias = "使用天数")
    private Integer useDays;
    private String drugForm;
    @ItemProperty(alias = "前端展示的药品名拼接名")
    private String drugDisplaySplicedName;

    public String getUseDoseStr() {
        return useDoseStr;
    }

    public void setUseDoseStr(String useDoseStr) {
        this.useDoseStr = useDoseStr;
    }

    public Integer getHisrecipedetailID() {
        return hisrecipedetailID;
    }

    public void setHisrecipedetailID(Integer hisrecipedetailID) {
        this.hisrecipedetailID = hisrecipedetailID;
    }

    public Integer getHisRecipeId() {
        return hisRecipeId;
    }

    public void setHisRecipeId(Integer hisRecipeId) {
        this.hisRecipeId = hisRecipeId;
    }

    public String getRecipeDeatilCode() {
        return recipeDeatilCode;
    }

    public void setRecipeDeatilCode(String recipeDeatilCode) {
        this.recipeDeatilCode = recipeDeatilCode;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getSaleName() {
        return saleName;
    }

    public void setSaleName(String saleName) {
        this.saleName = saleName;
    }

    public String getDrugSpec() {
        return drugSpec;
    }

    public void setDrugSpec(String drugSpec) {
        this.drugSpec = drugSpec;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getStandardCode() {
        return standardCode;
    }

    public void setStandardCode(String standardCode) {
        this.standardCode = standardCode;
    }

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public String getProducerCode() {
        return producerCode;
    }

    public void setProducerCode(String producerCode) {
        this.producerCode = producerCode;
    }

    public BigDecimal getUseTotalDose() {
        return useTotalDose;
    }

    public void setUseTotalDose(BigDecimal useTotalDose) {
        this.useTotalDose = useTotalDose;
    }

    public String getDrugUnit() {
        return drugUnit;
    }

    public void setDrugUnit(String drugUnit) {
        this.drugUnit = drugUnit;
    }

    public String getUseDose() {
        return useDose;
    }

    public void setUseDose(String useDose) {
        this.useDose = useDose;
    }

    public Integer getPack() {
        return pack;
    }

    public void setPack(Integer pack) {
        this.pack = pack;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getUsingRate() {
        return usingRate;
    }

    public void setUsingRate(String usingRate) {
        this.usingRate = usingRate;
    }

    public String getUsePathways() {
        return usePathways;
    }

    public void setUsePathways(String usePathways) {
        this.usePathways = usePathways;
    }

    public String getUsingRateText() {
        return usingRateText;
    }

    public void setUsingRateText(String usingRateText) {
        this.usingRateText = usingRateText;
    }

    public String getUsePathwaysText() {
        return usePathwaysText;
    }

    public void setUsePathwaysText(String usePathwaysText) {
        this.usePathwaysText = usePathwaysText;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getUseDays() {
        return useDays;
    }

    public void setUseDays(Integer useDays) {
        this.useDays = useDays;
    }

    public String getDrugForm() {
        return drugForm;
    }

    public void setDrugForm(String drugForm) {
        this.drugForm = drugForm;
    }

    public String getDrugDisplaySplicedName() {
        return drugDisplaySplicedName;
    }

    public void setDrugDisplaySplicedName(String drugDisplaySplicedName) {
        this.drugDisplaySplicedName = drugDisplaySplicedName;
    }
}
