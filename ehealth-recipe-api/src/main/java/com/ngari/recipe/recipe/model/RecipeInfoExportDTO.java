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
public class RecipeInfoExportDTO implements Serializable {
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
    private BigDecimal recipeFee;
    private Integer checker;
    private Date checkDateYs;
    @Dictionary(id = "eh.cdr.dictionary.FromFlag")
    private Integer fromflag;
    @Dictionary(id = "eh.cdr.dictionary.RecipeStatus")
    private Integer status;
    @ItemProperty(alias = "处方父状态：0：默认 ， 1：待提交，2：待审核，3：待够药，4：待发药，5：配送中，6：待取药，7：已完成，8：已删除 ，9：已作废")
    @Dictionary(id = "eh.recipe.recipeState.process")
    private Integer processState;
    @ItemProperty(alias = "处方子状态")
    private Integer subState;

    private Date payTime;
    private String doctorName;
    private Integer sumDose;
    private Integer sendType;
    private String outTradeNo;
    @Dictionary(id = "eh.cdr.dictionary.RecipeType")
    private Integer recipeType;

    @Dictionary(id = "eh.cdr.dictionary.BussSourceType")
    private Integer bussSource;

    @ItemProperty(alias = "处方单号")
    private String recipeCode;

    @ItemProperty(alias = "结算方式（医保 自费）")
    private Integer orderType;

    @ItemProperty(alias = "医保金额")
    private Double fundAmount;
    @ItemProperty(alias = "自费金额")
    private Double cashAmount;

    @ItemProperty(alias = "发药药师")
    private String giveUser;

    @ItemProperty(alias = "发药时间")
    private Date dispensingTime;

    @ItemProperty(alias = "处方业务类型  1 门诊处方  2  复诊处方  3 其他处方")
    private Integer recipeBusinessType;

    @ItemProperty(alias = "药企编码")
    private String enterpriseId;

    @ItemProperty(alias = "药店或者站点名称")
    private String drugStoreName;

    @ItemProperty(alias ="复诊类别")
    private Integer fastRecipeFlag;

    @ItemProperty(alias = "线下处方名称")
    private String offlineRecipeName;

    @ItemProperty(alias = "剂数")
    private Integer copyNum;

    public String getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(String enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public String getDrugStoreName() {
        return drugStoreName;
    }

    public void setDrugStoreName(String drugStoreName) {
        this.drugStoreName = drugStoreName;
    }

    public Integer getProcessState() {
        return processState;
    }

    public void setProcessState(Integer processState) {
        this.processState = processState;
    }

    public Integer getSubState() {
        return subState;
    }

    public void setSubState(Integer subState) {
        this.subState = subState;
    }

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

    public BigDecimal getRecipeFee() {
        return recipeFee;
    }

    public void setRecipeFee(BigDecimal recipeFee) {
        this.recipeFee = recipeFee;
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

    public Integer getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(Integer recipeType) {
        this.recipeType = recipeType;
    }

    public String getGiveUser() {
        return giveUser;
    }

    public void setGiveUser(String giveUser) {
        this.giveUser = giveUser;
    }

    public Date getDispensingTime() {
        return dispensingTime;
    }

    public void setDispensingTime(Date dispensingTime) {
        this.dispensingTime = dispensingTime;
    }

    public Integer getBussSource() {
        return bussSource;
    }

    public void setBussSource(Integer bussSource) {
        this.bussSource = bussSource;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public Integer getRecipeBusinessType() {
        return recipeBusinessType;
    }

    public void setRecipeBusinessType(Integer recipeBusinessType) {
        this.recipeBusinessType = recipeBusinessType;
    }

    public Integer getFastRecipeFlag() {
        return fastRecipeFlag;
    }

    public void setFastRecipeFlag(Integer fastRecipeFlag) {
        this.fastRecipeFlag = fastRecipeFlag;
    }

    public String getOfflineRecipeName() {
        return offlineRecipeName;
    }

    public void setOfflineRecipeName(String offlineRecipeName) {
        this.offlineRecipeName = offlineRecipeName;
    }

    public Integer getCopyNum() {
        return copyNum;
    }

    public void setCopyNum(Integer copyNum) {
        this.copyNum = copyNum;
    }
}
