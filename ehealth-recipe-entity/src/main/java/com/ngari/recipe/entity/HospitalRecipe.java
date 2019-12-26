package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author yinsheng
 * @date 2019\12\19 0019 14:43
 */
@Schema
@Entity
@Table(name = "cdr_hospitalRecipe")
@Access(AccessType.PROPERTY)
public class HospitalRecipe implements Serializable{
    private static final long serialVersionUID = -3232349061510103903L;

    @ItemProperty(alias="自增ID")
    private Integer hospitalRecipeID;
    @ItemProperty(alias="处方号")
    private String recipeCode;
    @ItemProperty(alias="就诊序号")
    private String clinicId;
    @ItemProperty(alias="患者id")
    private String patientId;
    @ItemProperty(alias="证件号")
    private String certificate;
    @ItemProperty(alias="患者姓名")
    private String patientName;
    @ItemProperty(alias="患者电话")
    private String patientTel;
    @ItemProperty(alias="患者医院病历号")
    private String patientNumber;
    @ItemProperty(alias="门诊号/挂号序号")
    private String registerId;
    @ItemProperty(alias="患者性别")
    private String patientSex;
    @ItemProperty(alias="开方机构编号")
    private String clinicOrgan;
    @ItemProperty(alias="组织机构编码")
    private String organId;
    @ItemProperty(alias="开方医生工号")
    private String doctorNumber;
    @ItemProperty(alias="开方医生姓名")
    private String doctorName;
    @ItemProperty(alias="开方时间")
    private String createDate;
    @ItemProperty(alias="创建时间")
    private Date createTime;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "hospitalRecipeID", nullable = false)
    public Integer getHospitalRecipeID() {
        return hospitalRecipeID;
    }

    public void setHospitalRecipeID(Integer hospitalRecipeID) {
        this.hospitalRecipeID = hospitalRecipeID;
    }

    @Column(name = "recipeCode")
    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    @Column(name = "clinicId")
    public String getClinicId() {
        return clinicId;
    }

    public void setClinicId(String clinicId) {
        this.clinicId = clinicId;
    }

    @Column(name = "patientId")
    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    @Column(name = "certificate")
    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    @Column(name = "patientName")
    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    @Column(name = "patientTel")
    public String getPatientTel() {
        return patientTel;
    }

    public void setPatientTel(String patientTel) {
        this.patientTel = patientTel;
    }

    @Column(name = "patientNumber")
    public String getPatientNumber() {
        return patientNumber;
    }

    public void setPatientNumber(String patientNumber) {
        this.patientNumber = patientNumber;
    }

    @Column(name = "registerId")
    public String getRegisterId() {
        return registerId;
    }

    public void setRegisterId(String registerId) {
        this.registerId = registerId;
    }

    @Column(name = "patientSex")
    public String getPatientSex() {
        return patientSex;
    }

    public void setPatientSex(String patientSex) {
        this.patientSex = patientSex;
    }

    @Column(name = "clinicOrgan")
    public String getClinicOrgan() {
        return clinicOrgan;
    }

    public void setClinicOrgan(String clinicOrgan) {
        this.clinicOrgan = clinicOrgan;
    }

    @Column(name = "organId")
    public String getOrganId() {
        return organId;
    }

    public void setOrganId(String organId) {
        this.organId = organId;
    }

    @Column(name = "doctorNumber")
    public String getDoctorNumber() {
        return doctorNumber;
    }

    public void setDoctorNumber(String doctorNumber) {
        this.doctorNumber = doctorNumber;
    }

    @Column(name = "doctorName")
    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    @Column(name = "createDate")
    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    @Column(name = "createTime")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
