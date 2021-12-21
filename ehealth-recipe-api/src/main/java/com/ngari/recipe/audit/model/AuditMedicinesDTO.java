package com.ngari.recipe.audit.model;

import ctd.schema.annotation.ItemProperty;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * created by shiyuping on 2018/11/26
 */
public class AuditMedicinesDTO implements Serializable {
    private static final long serialVersionUID = 8790865932099663823L;

    @ItemProperty(alias = "自增id")
    private Integer id;

    @ItemProperty(alias = "处方id")
    private Integer recipeId;

    @ItemProperty(alias = "药品名")
    private String name;

    @ItemProperty(alias = "药品编码")
    private String code;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    @ItemProperty(alias = "逻辑删除,1删除，0正常")
    private Integer logicalDeleted;

    @ItemProperty(alias = "状态")
    private Integer status;

    @ItemProperty(alias = "备注")
    private String remark;

    @ItemProperty(alias = "智能审方药品问题列表")
    private List<AuditMedicineIssueDTO> auditMedicineIssues;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    public Integer getLogicalDeleted() {
        return logicalDeleted;
    }

    public void setLogicalDeleted(Integer logicalDeleted) {
        this.logicalDeleted = logicalDeleted;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<AuditMedicineIssueDTO> getAuditMedicineIssues() {
        return auditMedicineIssues;
    }

    public void setAuditMedicineIssues(List<AuditMedicineIssueDTO> auditMedicineIssues) {
        this.auditMedicineIssues = auditMedicineIssues;
    }
}
