package com.ngari.recipe.recipe.model;

import java.io.Serializable;

/**
 * created by shiyuping on 2019/12/16
 * @author shiyuping
 */
public class NoticePlatRecipeMedicalInfoDTO implements Serializable {
    private static final long serialVersionUID = -4341954806502970198L;
    private String organId;
    /**平台处方ID*/
    private String platRecipeId;
    /**His处方号*/
    private String recipeCode;
    /**处方上传状态*/
    private String uploadStatus;
    /**失败原因*/
    private String failureInfo;
    /**医院机构编码*/
    private String hospOrgCode;
    /**参保地统筹区*/
    private String insuredArea;
    /**医保请求串*/
    private String medicalSettleData;

    public String getOrganId() {
        return organId;
    }

    public void setOrganId(String organId) {
        this.organId = organId;
    }

    public String getPlatRecipeId() {
        return platRecipeId;
    }

    public void setPlatRecipeId(String platRecipeId) {
        this.platRecipeId = platRecipeId;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public String getUploadStatus() {
        return uploadStatus;
    }

    public void setUploadStatus(String uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public String getFailureInfo() {
        return failureInfo;
    }

    public void setFailureInfo(String failureInfo) {
        this.failureInfo = failureInfo;
    }

    public String getHospOrgCode() {
        return hospOrgCode;
    }

    public void setHospOrgCode(String hospOrgCode) {
        this.hospOrgCode = hospOrgCode;
    }

    public String getInsuredArea() {
        return insuredArea;
    }

    public void setInsuredArea(String insuredArea) {
        this.insuredArea = insuredArea;
    }

    public String getMedicalSettleData() {
        return medicalSettleData;
    }

    public void setMedicalSettleData(String medicalSettleData) {
        this.medicalSettleData = medicalSettleData;
    }
}
