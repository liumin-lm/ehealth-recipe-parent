package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * created by shiyuping on 2019/5/29
 */
@Schema
public class HisRecipeDetailBean implements Serializable {

    private static final long serialVersionUID = 3550340367067978212L;
    private String recipeDeatilCode;
    private Integer recipeDetailId;
    private String drugCode;
    private String drugName;
    private BigDecimal price;
    @ItemProperty(alias = "开药总数")
    private Double useTotalDose;
    @ItemProperty(alias = "药品总价")
    private BigDecimal totalPrice;
    @ItemProperty(alias = "用药频次")
    private String usingRate;
    @ItemProperty(alias = "用药途径")
    private String usePathways;
    @ItemProperty(alias = "频次名称")
    private String usingRateText;
    @ItemProperty(alias = "途径名称")
    private String usePathwaysText;
    @ItemProperty(alias = "剂量单位")
    private String useDoseUnit;
    @ItemProperty(alias = "每次剂量")
    private String useDose;
    @ItemProperty(alias = "药品包装单位")
    private String drugUnit;
    @ItemProperty(alias = "开药天数")
    private String useDays;
    @ItemProperty(alias = "开药天数 带有小数")
    private String useDaysB;
    @ItemProperty(alias = "药品规格")
    private String drugSpec;
    @ItemProperty(alias = "药店编码")
    private String pharmacyCode;
    @ItemProperty(alias = "药品嘱托编码")
    private String drugEntrustCode;
    @ItemProperty(alias = "药品嘱托信息")
    private String memo;
    @ItemProperty(alias = "前端展示的药品拼接名")
    private String drugDisplaySplicedName;
    @ItemProperty(alias = "剂型")
    private String drugForm;
    @ItemProperty(alias = "药品所属类型 3 保密药品")
    private Integer type;

    public String getUseDaysB() {
        return useDaysB;
    }

    public void setUseDaysB(String useDaysB) {
        this.useDaysB = useDaysB;
    }

    public String getRecipeDeatilCode() {
        return recipeDeatilCode;
    }

    public void setRecipeDeatilCode(String recipeDeatilCode) {
        this.recipeDeatilCode = recipeDeatilCode;
    }

    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Double getUseTotalDose() {
        return useTotalDose;
    }

    public void setUseTotalDose(Double useTotalDose) {
        this.useTotalDose = useTotalDose;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
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

    public String getUseDoseUnit() {
        return useDoseUnit;
    }

    public void setUseDoseUnit(String useDoseUnit) {
        this.useDoseUnit = useDoseUnit;
    }

    public String getUseDose() {
        return useDose;
    }

    public void setUseDose(String useDose) {
        this.useDose = useDose;
    }

    public String getDrugUnit() {
        return drugUnit;
    }

    public void setDrugUnit(String drugUnit) {
        this.drugUnit = drugUnit;
    }

    public String getUseDays() {
        return useDays;
    }

    public void setUseDays(String useDays) {
        this.useDays = useDays;
    }

    public String getDrugSpec() {
        return drugSpec;
    }

    public void setDrugSpec(String drugSpec) {
        this.drugSpec = drugSpec;
    }

    public Integer getRecipeDetailId() {
        return recipeDetailId;
    }

    public void setRecipeDetailId(Integer recipeDetailId) {
        this.recipeDetailId = recipeDetailId;
    }

    public String getPharmacyCode() {
        return pharmacyCode;
    }

    public void setPharmacyCode(String pharmacyCode) {
        this.pharmacyCode = pharmacyCode;
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

    public String getDrugDisplaySplicedName() {
        return drugDisplaySplicedName;
    }

    public void setDrugDisplaySplicedName(String drugDisplaySplicedName) {
        this.drugDisplaySplicedName = drugDisplaySplicedName;
    }

    public String getDrugEntrustCode() {
        return drugEntrustCode;
    }

    public void setDrugEntrustCode(String drugEntrustCode) {
        this.drugEntrustCode = drugEntrustCode;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getDrugForm() {
        return drugForm;
    }

    public void setDrugForm(String drugForm) {
        this.drugForm = drugForm;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
}
