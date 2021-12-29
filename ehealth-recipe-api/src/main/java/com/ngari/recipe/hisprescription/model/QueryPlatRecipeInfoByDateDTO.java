package com.ngari.recipe.hisprescription.model;

import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * created by shiyuping on 2020/2/10
 */
public class QueryPlatRecipeInfoByDateDTO implements Serializable {
    private static final long serialVersionUID = -7552686754198494904L;

    /**
     * 平台机构id
     */
    private Integer organId;

    /**
     * 平台处方id
     */
    private String recipeId;
    /**
     * 患者姓名
     */
    private String patientName;
    /**
     * 证件类型
     */
    private String certificateType;
    /**
     * 证件号
     */
    @Desensitizations(type = DesensitizationsType.IDCARD)
    private String certificate;
    /**
     * 卡类型
     */
    private String cardType;
    /**
     * 卡号
     */
    @Desensitizations(type = DesensitizationsType.HEALTHCARD)
    private String cardNo;
    /**
     * 查询开始时间
     */
    private Date startDate;
    /**
     * 查询结束时间
     */
    private Date endDate;
    /**
     * 医保入参
     */
    private String medicalInsuranceParam;
    //预留参数
    private Map<String, Object> params;

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getMedicalInsuranceParam() {
        return medicalInsuranceParam;
    }

    public void setMedicalInsuranceParam(String medicalInsuranceParam) {
        this.medicalInsuranceParam = medicalInsuranceParam;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public String getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(String certificateType) {
        this.certificateType = certificateType;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }
}
