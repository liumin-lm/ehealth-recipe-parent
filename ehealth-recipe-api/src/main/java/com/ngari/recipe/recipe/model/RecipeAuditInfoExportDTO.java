package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;


@Entity
@Schema
@Access(AccessType.PROPERTY)
public class RecipeAuditInfoExportDTO implements Serializable {
    private static final long serialVersionUID = 2739705893333991122L;

    private Integer recipeId;

    @ItemProperty(alias = "处方单号")
    private String recipeCode;

    @ItemProperty(alias = "患者姓名")
    private String patientName;

    @ItemProperty(alias = "患者mpiId")
    private String mpiId;

    @ItemProperty(alias = "开方医生")
    private Integer doctor;

    @ItemProperty(alias = "开方医生")
    private String doctorName;

    @ItemProperty(alias = "签名时间")
    private Date signDate;

    @ItemProperty(alias = "开方机构")
    private Integer clinicOrgan;

    @ItemProperty(alias = "医院名")
    private String organName;

    @ItemProperty(alias = "人工审核日期")
    private Date checkDateYs;

    @ItemProperty(alias = "审核药师姓名")
    private String checkerText;

    @ItemProperty(alias = "审核状态")
    private String checkFlag;

    @ItemProperty(alias = "审核模式")
    private Integer reviewType;

    //主要用于运营平台查询使用
    @ItemProperty(alias = "是否自动审核 1自动审核，0/null药师审核")
    private Integer autoCheck;


    @Id
    @GeneratedValue(strategy = IDENTITY)
    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getMpiId() {
        return mpiId;
    }

    public void setMpiId(String mpiId) {
        this.mpiId = mpiId;
    }

    public Integer getDoctor() {
        return doctor;
    }

    public void setDoctor(Integer doctor) {
        this.doctor = doctor;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public Date getSignDate() {
        return signDate;
    }

    public void setSignDate(Date signDate) {
        this.signDate = signDate;
    }

    public Integer getClinicOrgan() {
        return clinicOrgan;
    }

    public void setClinicOrgan(Integer clinicOrgan) {
        this.clinicOrgan = clinicOrgan;
    }

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public Date getCheckDateYs() {
        return checkDateYs;
    }

    public void setCheckDateYs(Date checkDateYs) {
        this.checkDateYs = checkDateYs;
    }

    public String getCheckerText() {
        return checkerText;
    }

    public void setCheckerText(String checkerText) {
        this.checkerText = checkerText;
    }

    public String getCheckFlag() {
        return checkFlag;
    }

    public void setCheckFlag(String checkFlag) {
        this.checkFlag = checkFlag;
    }

    public Integer getReviewType() {
        return reviewType;
    }

    public void setReviewType(Integer reviewType) {
        this.reviewType = reviewType;
    }

    public Integer getAutoCheck() {
        return autoCheck;
    }

    public void setAutoCheck(Integer autoCheck) {
        this.autoCheck = autoCheck;
    }
}
