package com.ngari.recipe.recipe.model;

import java.io.Serializable;
import java.util.Date;

public class MedicInsurSettleSuccNoticNgariReqDTO implements Serializable{
    private static final long serialVersionUID = 2897574495021606011L;
    private Integer organId;
    private String organName;
    private String certId;
    private String patientName;
    private String recipeId; //平台处方号
    private String recipeCode; //his处方号
    private String payStatus;//0：未结算支付 1：已提交结算2：已结算支付9：结算支付失败
    private String invoiceId;//HIS发票号
    private Double totalAmount; //总金额（本次就诊首张处方含诊察费）
    private Double fundAmount; //处方医保支付
    private Double cashAmount; //处方自费支付
    private String InsuTSN; //医保结算流水号
    private String payOrderNo; //第三方支付订单号
    private Date SettlingTime; //医保结算时间 yyyy-MM-dd HH:mm:ss

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public String getCertId() {
        return certId;
    }

    public void setCertId(String certId) {
        this.certId = certId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public String getPayStatus() {
        return payStatus;
    }

    public void setPayStatus(String payStatus) {
        this.payStatus = payStatus;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
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

    public String getInsuTSN() {
        return InsuTSN;
    }

    public void setInsuTSN(String insuTSN) {
        InsuTSN = insuTSN;
    }

    public String getPayOrderNo() {
        return payOrderNo;
    }

    public void setPayOrderNo(String payOrderNo) {
        this.payOrderNo = payOrderNo;
    }

    public Date getSettlingTime() {
        return SettlingTime;
    }

    public void setSettlingTime(Date settlingTime) {
        SettlingTime = settlingTime;
    }
}
