package com.ngari.recipe.hisprescription.model;

import java.io.Serializable;

/**
 * created by shiyuping on 2019/11/8
 * @author shiyuping
 */
public class HosPatientRecipeDTO implements Serializable {
    private static final long serialVersionUID = -2725076945305467441L;
    /**
     * 机构id
     */
    private String organId;
    /**
     * 处方信息
     */
    private HosRecipeDTO recipe;
    /**
     * 患者信息
     */
    private HosPatientDTO patient;
    /**
     * 就诊编号
     */
    private String clinicNo;

    //reqType 请求类型（1：二维码扫码推送详情 2：自动推送详情链接跳转请求 ）
    private Integer reqType;

    public HosRecipeDTO getRecipe() {
        return recipe;
    }

    public void setRecipe(HosRecipeDTO recipe) {
        this.recipe = recipe;
    }

    public HosPatientDTO getPatient() {
        return patient;
    }

    public void setPatient(HosPatientDTO patient) {
        this.patient = patient;
    }

    public String getClinicNo() {
        return clinicNo;
    }

    public void setClinicNo(String clinicNo) {
        this.clinicNo = clinicNo;
    }

    public Integer getReqType() {
        return reqType;
    }

    public void setReqType(Integer reqType) {
        this.reqType = reqType;
    }

    public String getOrganId() {
        return organId;
    }

    public void setOrganId(String organId) {
        this.organId = organId;
    }
}
