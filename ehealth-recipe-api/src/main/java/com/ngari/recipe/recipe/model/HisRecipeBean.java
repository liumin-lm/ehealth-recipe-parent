package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * created by shiyuping on 2019/5/29
 */
@Schema
public class HisRecipeBean implements Serializable {
    private static final long serialVersionUID = 8133021203816048936L;
    @ItemProperty(alias = "开方机构")
    @Dictionary(id = "eh.base.dictionary.Organ")
    private Integer clinicOrgan;
    @ItemProperty(alias = "开方机构名称")
    private String organName;
    private String registeredId;
    private String recipeCode;
    private String organDiseaseName;
    private String recipeType;
    private String signDate;
    private String departCode;
    private String departText;
    private String doctorCode;
    private String doctorName;
    private Integer checkStatus;
    private List<HisRecipeDetailBean> detailData;
    private Integer queryStatus;
    private String serialNumber;
    private Integer copyNum;
    private String recipeMemo;
    private RecipeExtendBean recipeExtend;

    public Integer getQueryStatus() {
        return queryStatus;
    }

    public void setQueryStatus(Integer queryStatus) {
        this.queryStatus = queryStatus;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getRegisteredId() {
        return registeredId;
    }

    public void setRegisteredId(String registeredId) {
        this.registeredId = registeredId;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public String getOrganDiseaseName() {
        return organDiseaseName;
    }

    public void setOrganDiseaseName(String organDiseaseName) {
        this.organDiseaseName = organDiseaseName;
    }

    public String getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(String recipeType) {
        this.recipeType = recipeType;
    }

    public String getSignDate() {
        return signDate;
    }

    public void setSignDate(String signDate) {
        this.signDate = signDate;
    }

    public String getDepartCode() {
        return departCode;
    }

    public void setDepartCode(String departCode) {
        this.departCode = departCode;
    }

    public String getDepartText() {
        return departText;
    }

    public void setDepartText(String departText) {
        this.departText = departText;
    }

    public String getDoctorCode() {
        return doctorCode;
    }

    public void setDoctorCode(String doctorCode) {
        this.doctorCode = doctorCode;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public Integer getCheckStatus() {
        return checkStatus;
    }

    public void setCheckStatus(Integer checkStatus) {
        this.checkStatus = checkStatus;
    }

    public List<HisRecipeDetailBean> getDetailData() {
        return detailData;
    }

    public void setDetailData(List<HisRecipeDetailBean> detailData) {
        this.detailData = detailData;
    }

    public Integer getClinicOrgan() {
        return clinicOrgan;
    }

    public void setClinicOrgan(Integer clinicOrgan) {
        this.clinicOrgan = clinicOrgan;
    }

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public Integer getCopyNum() {
        return copyNum;
    }

    public void setCopyNum(Integer copyNum) {
        this.copyNum = copyNum;
    }

    public String getRecipeMemo() {
        return recipeMemo;
    }

    public void setRecipeMemo(String recipeMemo) {
        this.recipeMemo = recipeMemo;
    }

    public RecipeExtendBean getRecipeExtend() {
        return recipeExtend;
    }

    public void setRecipeExtend(RecipeExtendBean recipeExtend) {
        this.recipeExtend = recipeExtend;
    }
}
