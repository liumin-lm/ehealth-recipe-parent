package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author： 0184/yu_yun
 * @date： 2018/6/28
 * @description： 医院处方记录
 * @version： 1.0
 */
@Schema
@Entity
@Table(name = "cdr_hisprescription")
@Access(AccessType.PROPERTY)
public class Hisprescription implements Serializable{

    private static final long serialVersionUID = -6018038996286418418L;

    @ItemProperty(alias="处方ID")
    private Integer recipeId;

    @ItemProperty(alias="机构编码")
    private String organId;

    @ItemProperty(alias="院区代码")
    private String hoscode;

    @ItemProperty(alias="医院处方编号")
    private String recipeNo;

    @ItemProperty(alias="处方名称")
    private String recipeName;

    @ItemProperty(alias="1西药  2中成药")
    private Integer recipeType;

    @ItemProperty(alias="处方日期")
    private Date recipeDate;

    @ItemProperty(alias="诊断")
    private String icdName;

    @ItemProperty(alias="处方备注")
    private String remark;

    @ItemProperty(alias="处方金额")
    private BigDecimal amount;

    @ItemProperty(alias="总金额")
    private BigDecimal totalAmount;

    @ItemProperty(alias="卡(病历)号码")
    private String cardNo;

    @ItemProperty(alias="身份证")
    private String certID;

    @ItemProperty(alias="就诊病人姓名")
    private String patientName;

    @ItemProperty(alias="病人性别(1：男 2：女)")
    private Integer patientSex;

    @ItemProperty(alias="出生日期")
    private Date birthday;

    @ItemProperty(alias="年龄")
    private Integer age;

    @ItemProperty(alias="病人类型")
    private String patientType;

    @ItemProperty(alias="病人手机号")
    private String patientMobile;

    @ItemProperty(alias="开单科室（挂号科室）")
    private String deptId;

    @ItemProperty(alias="处方医生工号")
    private String doctorId;

    @ItemProperty(alias="处方医生姓名")
    private String doctorName;

    @ItemProperty(alias="1支付 0未支付")
    private Integer isPay;

    @ItemProperty(alias="0无效 1 有效 8已取消")
    private Integer recipeStatus;

    @ItemProperty(alias="0未发药 1已发药")
    private Integer phStatus;

    @ItemProperty(alias="1医院内部处方 2医院外配处方 3纳里健康平台处方")
    private Integer recipeProperty;

    @ItemProperty(alias="0医院药房取药 1物流配送 2药店取药")
    private Integer deliveryType;

    @ItemProperty(alias="配送地址")
    private String deliveryAd;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "recipeId", nullable = false)
    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    @Column(name = "organId")
    public String getOrganId() {
        return organId;
    }

    public void setOrganId(String organId) {
        this.organId = organId;
    }

    @Column(name = "hoscode")
    public String getHoscode() {
        return hoscode;
    }

    public void setHoscode(String hoscode) {
        this.hoscode = hoscode;
    }

    @Column(name = "recipeNo")
    public String getRecipeNo() {
        return recipeNo;
    }

    public void setRecipeNo(String recipeNo) {
        this.recipeNo = recipeNo;
    }

    @Column(name = "recipeName")
    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    @Column(name = "recipeType")
    public Integer getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(Integer recipeType) {
        this.recipeType = recipeType;
    }

    @Column(name = "recipeDate")
    public Date getRecipeDate() {
        return recipeDate;
    }

    public void setRecipeDate(Date recipeDate) {
        this.recipeDate = recipeDate;
    }

    @Column(name = "icdName")
    public String getIcdName() {
        return icdName;
    }

    public void setIcdName(String icdName) {
        this.icdName = icdName;
    }

    @Column(name = "remark")
    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Column(name = "amount")
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Column(name = "totalAmount")
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    @Column(name = "cardNo")
    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    @Column(name = "certID")
    public String getCertID() {
        return certID;
    }

    public void setCertID(String certID) {
        this.certID = certID;
    }

    @Column(name = "patientName")
    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    @Column(name = "patientSex")
    public Integer getPatientSex() {
        return patientSex;
    }

    public void setPatientSex(Integer patientSex) {
        this.patientSex = patientSex;
    }

    @Column(name = "birthday")
    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    @Column(name = "age")
    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    @Column(name = "patientType")
    public String getPatientType() {
        return patientType;
    }

    public void setPatientType(String patientType) {
        this.patientType = patientType;
    }

    @Column(name = "patientMobile")
    public String getPatientMobile() {
        return patientMobile;
    }

    public void setPatientMobile(String patientMobile) {
        this.patientMobile = patientMobile;
    }

    @Column(name = "deptId")
    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    @Column(name = "doctorId")
    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    @Column(name = "doctorName")
    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    @Column(name = "isPay")
    public Integer getIsPay() {
        return isPay;
    }

    public void setIsPay(Integer isPay) {
        this.isPay = isPay;
    }

    @Column(name = "recipeStatus")
    public Integer getRecipeStatus() {
        return recipeStatus;
    }

    public void setRecipeStatus(Integer recipeStatus) {
        this.recipeStatus = recipeStatus;
    }

    @Column(name = "phStatus")
    public Integer getPhStatus() {
        return phStatus;
    }

    public void setPhStatus(Integer phStatus) {
        this.phStatus = phStatus;
    }

    @Column(name = "recipeProperty")
    public Integer getRecipeProperty() {
        return recipeProperty;
    }

    public void setRecipeProperty(Integer recipeProperty) {
        this.recipeProperty = recipeProperty;
    }

    @Column(name = "deliveryType")
    public Integer getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(Integer deliveryType) {
        this.deliveryType = deliveryType;
    }

    @Column(name = "deliveryAd")
    public String getDeliveryAd() {
        return deliveryAd;
    }

    public void setDeliveryAd(String deliveryAd) {
        this.deliveryAd = deliveryAd;
    }
}
