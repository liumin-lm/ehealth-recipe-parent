package com.ngari.recipe.hisprescription.model;

import java.io.Serializable;

/**
 * 医院处方查询条件对象
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * date:2017/5/5.
 */
//liumin delete 调用处方法已做法，就不脱敏了
@Deprecated
public class HospitalSearchQO implements Serializable {
    private static final long serialVersionUID = -1890327956837342263L;

    /**
     * 机构组织代码（平台约定）
     */
    private String clinicOrgan;

    /**
     * 处方编号
     */
    private String recipeCode;

    /**
     * 卡类型（1医院就诊卡  2医保卡3 医院病历号）
     */
    private String cardType;

    /**
     * 卡(病历)号码
     */
    private String cardNo;

    /**
     * 患者身份证
     */
    private String certID;

    /**
     * 患者姓名
     */
    private String patientName;

    /**
     * 手机号码
     */
    private String mobile;

    /**
     * 处方医生工号
     */
    private String doctorID;

    /**
     * 处方开始日期
     */
    private String startDate;

    /**
     * 处方结束日期
     */
    private String endDate;

    public String getClinicOrgan() {
        return clinicOrgan;
    }

    public void setClinicOrgan(String clinicOrgan) {
        this.clinicOrgan = clinicOrgan;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
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

    public String getCertID() {
        return certID;
    }

    public void setCertID(String certID) {
        this.certID = certID;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getDoctorID() {
        return doctorID;
    }

    public void setDoctorID(String doctorID) {
        this.doctorID = doctorID;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
}
