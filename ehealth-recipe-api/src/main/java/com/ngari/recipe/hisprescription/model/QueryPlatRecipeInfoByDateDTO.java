package com.ngari.recipe.hisprescription.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * created by shiyuping on 2020/2/10
 */
public class QueryPlatRecipeInfoByDateDTO implements Serializable {
    private static final long serialVersionUID = -7552686754198494904L;
    /**
     * 患者姓名
     */
    private String patientName;
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
    private Map<String,Object> params;

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
}
