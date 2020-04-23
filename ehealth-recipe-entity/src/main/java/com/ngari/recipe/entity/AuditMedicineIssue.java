package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * created by shiyuping on 2018/11/23
 */
@Schema
@Entity
@Table(name = "cdr_auditmedicine_issue")
@Access(AccessType.PROPERTY)
public class AuditMedicineIssue implements Serializable {
    private static final long serialVersionUID = 4012648640329779968L;

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

    @ItemProperty(alias = "处方分析明细")
    private String detailUrl;

    @Column(name = "createTime")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Column(name = "lastModify")
    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    @Column(name = "logicalDeleted")
    public Integer getLogicalDeleted() {
        return logicalDeleted;
    }

    public void setLogicalDeleted(Integer logicalDeleted) {
        this.logicalDeleted = logicalDeleted;
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "issueId", unique = true, nullable = false)
    public Integer getIssueId() {
        return issueId;
    }

    public void setIssueId(Integer issueId) {
        this.issueId = issueId;
    }

    @Column(name = "lvl")
    public String getLvl() {
        return lvl;
    }

    public void setLvl(String lvl) {
        this.lvl = lvl;
    }

    @Column(name = "lvlCode")
    public String getLvlCode() {
        return lvlCode;
    }

    public void setLvlCode(String lvlCode) {
        this.lvlCode = lvlCode;
    }

    @Column(name = "detail")
    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    @Column(name = "title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Column(name = "medicineId")
    public Integer getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(Integer medicineId) {
        this.medicineId = medicineId;
    }

    @Column(name = "recipeId")
    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    @Column(name = "detailUrl")
    public String getDetailUrl() {
        return detailUrl;
    }

    public void setDetailUrl(String detailUrl) {
        this.detailUrl = detailUrl;
    }
}
