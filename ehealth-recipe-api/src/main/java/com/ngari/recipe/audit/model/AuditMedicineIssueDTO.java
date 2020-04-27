package com.ngari.recipe.audit.model;

import ctd.schema.annotation.ItemProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * created by shiyuping on 2018/11/26
 */
public class AuditMedicineIssueDTO implements Serializable {
    private static final long serialVersionUID = 3348418120928947432L;

    @ItemProperty(alias="自动审方结果药品问题id")
    private Integer issueId;

    @ItemProperty(alias="自动审方结果药品id")
    private Integer medicineId;

    @ItemProperty(alias="处方id")
    private Integer recipeId;

    @ItemProperty(alias="警示类型")
    private String lvl;

    @ItemProperty(alias="警示类型编码")
    private String lvlCode;

    @ItemProperty(alias="问题明细")
    private String detail;

    @ItemProperty(alias="问题标题")
    private String title;

    @ItemProperty(alias="创建时间")
    private Date createTime;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    @ItemProperty(alias = "逻辑删除")
    private Integer logicalDeleted;

    @ItemProperty(alias = "处方分析详情")
    private Integer detailUrl;

    public Integer getIssueId() {
        return issueId;
    }

    public void setIssueId(Integer issueId) {
        this.issueId = issueId;
    }

    public Integer getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(Integer medicineId) {
        this.medicineId = medicineId;
    }

    public String getLvl() {
        return lvl;
    }

    public void setLvl(String lvl) {
        this.lvl = lvl;
    }

    public String getLvlCode() {
        return lvlCode;
    }

    public void setLvlCode(String lvlCode) {
        this.lvlCode = lvlCode;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public Integer getDetailUrl() {
        return detailUrl;
    }

    public void setDetailUrl(Integer detailUrl) {
        this.detailUrl = detailUrl;
    }
}
