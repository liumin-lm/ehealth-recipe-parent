package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;

import java.io.Serializable;


public class PatientInfoDTO implements Serializable {

    private String patientCode;
    /**
     * 卡号
     */
    @Desensitizations(type = DesensitizationsType.HEALTHCARD)
    private String card;
    /**
     * 卡类型（1身份证 2 医保卡 3 临时卡）
     */
    private Integer cardType;
    /**
     * 患者姓名
     */
    private String patientName;
    /**
     * 年龄（小于一岁时传递值xx月xx天 大于一岁直接数值字符串类型）
     */
    private String patientAge;
    /**
     * 性别 1男 2女
     */
    private Integer gender;
    /**
     * 就诊科室名称
     */
    private String deptName;
    /**
     * 就诊号
     */
    private String adminNo;
    /**
     * 标识字段（暂未定义）默认0
     */
    private Integer flag;
    /**
     * 处方开具时间 精确到秒
     */
    private String docDate;

    public String getPatientCode() {
        return patientCode;
    }

    public void setPatientCode(String patientCode) {
        this.patientCode = patientCode;
    }

    public String getCard() {
        return card;
    }

    public void setCard(String card) {
        this.card = card;
    }

    public Integer getCardType() {
        return cardType;
    }

    public void setCardType(Integer cardType) {
        this.cardType = cardType;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientAge() {
        return patientAge;
    }

    public void setPatientAge(String patientAge) {
        this.patientAge = patientAge;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getAdminNo() {
        return adminNo;
    }

    public void setAdminNo(String adminNo) {
        this.adminNo = adminNo;
    }

    public Integer getFlag() {
        return flag;
    }

    public void setFlag(Integer flag) {
        this.flag = flag;
    }

    public String getDocDate() {
        return docDate;
    }

    public void setDocDate(String docDate) {
        this.docDate = docDate;
    }
}
