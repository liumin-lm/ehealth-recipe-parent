package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Created by  on 2016/10/25 0025.
 * 处方审核记录
 * @author zhongzx
 */

@Schema
@Entity
@Table(name="cdr_recipecheck")
@Access(AccessType.PROPERTY)
public class RecipeCheck implements Serializable{

    private static final long serialVersionUID = -5674562784545982003L;

    @ItemProperty(alias = "审核序号")
    private Integer checkId;

    @ItemProperty(alias = "处方序号")
    private Integer recipeId;

    @ItemProperty(alias = "审核机构")
    @Dictionary(id = "eh.base.dictionary.Organ")
    private Integer checkOrgan;

    @ItemProperty(alias = "审核人")
    @Dictionary(id = "eh.base.dictionary.Doctor")
    private Integer checker;

    @ItemProperty(alias = "审核人姓名")
    private String checkerName;

    @ItemProperty(alias = "审核备注信息")
    private String memo;

    @ItemProperty(alias = "审核日期")
    private Date checkDate;

    @ItemProperty(alias = "审核结果")
    private Integer checkStatus;

    @ItemProperty(alias = "抢单医生id")
    private Integer grabDoctorId;

    @ItemProperty(alias = "抢单状态 0:未抢单 1:已抢单")
    private Integer grabOrderStatus;

    @ItemProperty(alias = "抢单自动解锁时间")
    private Date localLimitDate;

    @ItemProperty(alias = "更新时间")
    private Date updateTime;

    @Id
    @GeneratedValue(strategy=IDENTITY)
    @Column(name = "CheckId", unique = true, nullable = false)
    public Integer getCheckId() {
        return checkId;
    }

    public void setCheckId(Integer checkId) {
        this.checkId = checkId;
    }

    @Column(name = "CheckerName")
    public String getCheckerName() {
        return checkerName;
    }

    public void setCheckerName(String checkerName) {
        this.checkerName = checkerName;
    }

    @Column(name = "Memo", length = 200)
    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    @Column(name = "CheckOrgan", nullable = false)
    public Integer getCheckOrgan() {
        return checkOrgan;
    }

    public void setCheckOrgan(Integer checkOrgan) {
        this.checkOrgan = checkOrgan;
    }

    @Column(name = "RecipeId", nullable = false)
    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    @Column(name = "Checker", nullable = false)
    public Integer getChecker() {
        return checker;
    }

    public void setChecker(Integer checker) {
        this.checker = checker;
    }

    @Column(name = "CheckStatus")
    public Integer getCheckStatus() {
        return checkStatus;
    }

    public void setCheckStatus(Integer checkStatus) {
        this.checkStatus = checkStatus;
    }

    @Column(name = "CheckDate")
    public Date getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(Date checkDate) {
        this.checkDate = checkDate;
    }

    @Column(name = "grabDoctorId")
    public Integer getGrabDoctorId() {
        return grabDoctorId;
    }

    public void setGrabDoctorId(Integer grabDoctorId) {
        this.grabDoctorId = grabDoctorId;
    }

    @Column(name = "grabOrderStatus")
    public Integer getGrabOrderStatus() {
        return grabOrderStatus;
    }

    public void setGrabOrderStatus(Integer grabOrderStatus) {
        this.grabOrderStatus = grabOrderStatus;
    }

    @Column(name = "localLimitDate")
    public Date getLocalLimitDate() {
        return localLimitDate;
    }

    public void setLocalLimitDate(Date localLimitDate) {
        this.localLimitDate = localLimitDate;
    }

    @Column(name = "updateTime")
    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
