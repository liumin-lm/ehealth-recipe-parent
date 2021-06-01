package com.ngari.recipe.recipe.model;

import java.io.Serializable;
import java.util.Date;

/**
 * created by shiyuping on 2020/3/16
 */
public class NoticeNgariAuditResDTO implements Serializable {
    private static final long serialVersionUID = -6314837056852976123L;
    /** 机构id*/
    private Integer organId;
    /** his处方号*/
    private String recipeCode;
    /** 审核医生代码*/
    private String auditDoctorCode;
    /** 审核医生名称*/
    private String auditDoctorName;
    /** 审核时间*/
    private Date auditTime;
    /** 审核结果 0审核不通过 1审核通过 2审核失败*/
    private String auditResult;
    /** 审核结果描述*/
    private String memo;
    /** 本地处方号*/
    private String recipeId;

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public String getAuditDoctorCode() {
        return auditDoctorCode;
    }

    public void setAuditDoctorCode(String auditDoctorCode) {
        this.auditDoctorCode = auditDoctorCode;
    }

    public String getAuditDoctorName() {
        return auditDoctorName;
    }

    public void setAuditDoctorName(String auditDoctorName) {
        this.auditDoctorName = auditDoctorName;
    }

    public Date getAuditTime() {
        return auditTime;
    }

    public void setAuditTime(Date auditTime) {
        this.auditTime = auditTime;
    }

    public String getAuditResult() {
        return auditResult;
    }

    public void setAuditResult(String auditResult) {
        this.auditResult = auditResult;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }
}
