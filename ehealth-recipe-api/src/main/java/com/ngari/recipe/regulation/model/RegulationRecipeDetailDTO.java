package com.ngari.recipe.regulation.model;

import java.io.Serializable;

public class RegulationRecipeDetailDTO implements Serializable{
    private static final long serialVersionUID = 370873716651120967L;
    private Integer organId; //机构id
    private String recipeId; //处方号
    private String recipeDetailId; //处方项目明细号码
    private String medicalDrugCode; //项目标准代码
    private String drugFormCode; //剂型代码
    private String usePathways; //用药途径代码
    private String useDays; //用药天数
    private String useTotalDose; //发药数量
    private String drugUnit; //发药数量单位
    private String usingRate; //用药频次代码
    private String useDose; //每次使用剂量
    private String useDoseUnit; //每次使用剂量单位
    private String zydm; //中药煎煮法代码   非必填
    private String modifyFlag; //修改标志

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getRecipeDetailId() {
        return recipeDetailId;
    }

    public void setRecipeDetailId(String recipeDetailId) {
        this.recipeDetailId = recipeDetailId;
    }

    public String getMedicalDrugCode() {
        return medicalDrugCode;
    }

    public void setMedicalDrugCode(String medicalDrugCode) {
        this.medicalDrugCode = medicalDrugCode;
    }

    public String getDrugFormCode() {
        return drugFormCode;
    }

    public void setDrugFormCode(String drugFormCode) {
        this.drugFormCode = drugFormCode;
    }

    public String getUsePathways() {
        return usePathways;
    }

    public void setUsePathways(String usePathways) {
        this.usePathways = usePathways;
    }

    public String getUseDays() {
        return useDays;
    }

    public void setUseDays(String useDays) {
        this.useDays = useDays;
    }

    public String getUseTotalDose() {
        return useTotalDose;
    }

    public void setUseTotalDose(String useTotalDose) {
        this.useTotalDose = useTotalDose;
    }

    public String getDrugUnit() {
        return drugUnit;
    }

    public void setDrugUnit(String drugUnit) {
        this.drugUnit = drugUnit;
    }

    public String getUsingRate() {
        return usingRate;
    }

    public void setUsingRate(String usingRate) {
        this.usingRate = usingRate;
    }

    public String getUseDose() {
        return useDose;
    }

    public void setUseDose(String useDose) {
        this.useDose = useDose;
    }

    public String getUseDoseUnit() {
        return useDoseUnit;
    }

    public void setUseDoseUnit(String useDoseUnit) {
        this.useDoseUnit = useDoseUnit;
    }

    public String getZydm() {
        return zydm;
    }

    public void setZydm(String zydm) {
        this.zydm = zydm;
    }

    public String getModifyFlag() {
        return modifyFlag;
    }

    public void setModifyFlag(String modifyFlag) {
        this.modifyFlag = modifyFlag;
    }
}
