package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author Created by liuxiaofeng on 2020/12/10.
 */
@Entity
@Schema
@Access(AccessType.PROPERTY)
public class RecipeInfoExportDTO implements Serializable{
    private static final long serialVersionUID = 2739705893333991122L;

    private Integer recipeId;
    private String patientName;
    private String mpiId;
    private String organName;
    @ItemProperty(alias = "开方科室")
    @Dictionary(id = "eh.base.dictionary.Depart")
    private Integer depart;
    private Integer doctor;
    private String organDiseaseName;
    private BigDecimal totalMoney;
    private Integer checker;
    private Date checkDateYs;
    @Dictionary(id = "eh.cdr.dictionary.FromFlag")
    private Integer fromflag;
    @Dictionary(id = "eh.cdr.dictionary.RecipeStatus")
    private Integer status;
    private Date payTime;
    private String doctorName;
    private Integer sumDose;
    private  Integer sendType;
    private String outTradeNo;
    @Dictionary(id = "eh.cdr.dictionary.RecipeType")
    private Integer recipeType;

    @ItemProperty(alias = "结算方式（医保 自费）")
    private Integer orderType;

    @ItemProperty(alias = "医保金额")
    private Double fundAmount;
    @ItemProperty(alias = "自费金额")
    private Double cashAmount;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
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

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public Integer getDepart() {
        return depart;
    }

    public void setDepart(Integer depart) {
        this.depart = depart;
    }

    public Integer getDoctor() {
        return doctor;
    }

    public void setDoctor(Integer doctor) {
        this.doctor = doctor;
    }

    public String getOrganDiseaseName() {
        return organDiseaseName;
    }

    public void setOrganDiseaseName(String organDiseaseName) {
        this.organDiseaseName = organDiseaseName;
    }

    public BigDecimal getTotalMoney() {
        return totalMoney;
    }

    public void setTotalMoney(BigDecimal totalMoney) {
        this.totalMoney = totalMoney;
    }

    public Integer getChecker() {
        return checker;
    }

    public void setChecker(Integer checker) {
        this.checker = checker;
    }

    public Date getCheckDateYs() {
        return checkDateYs;
    }

    public void setCheckDateYs(Date checkDateYs) {
        this.checkDateYs = checkDateYs;
    }

    public Integer getFromflag() {
        return fromflag;
    }

    public void setFromflag(Integer fromflag) {
        this.fromflag = fromflag;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getPayTime() {
        return payTime;
    }

    public void setPayTime(Date payTime) {
        this.payTime = payTime;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public Integer getSumDose() {
        return sumDose;
    }

    public void setSumDose(Integer sumDose) {
        this.sumDose = sumDose;
    }

    public Integer getSendType() {
        return sendType;
    }

    public void setSendType(Integer sendType) {
        this.sendType = sendType;
    }

    public String getOutTradeNo() {
        return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo) {
        this.outTradeNo = outTradeNo;
    }

    public Integer getOrderType() {
        return orderType;
    }

    public void setOrderType(Integer orderType) {
        this.orderType = orderType;
    }

    public Double getFundAmount() {
        return fundAmount;
    }

    public void setFundAmount(Double fundAmount) {
        this.fundAmount = fundAmount;
    }

    public Double getCashAmount() {
        return cashAmount;
    }

    public void setCashAmount(Double cashAmount) {
        this.cashAmount = cashAmount;
    }
}
