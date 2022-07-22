package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author zgy
 * @date 2022/7/4 10:05
 */
@Entity
@Schema
@Access(AccessType.PROPERTY)
public class RecipeOrderDetailExportDTO implements Serializable {
    private static final long serialVersionUID = -8299461454513863587L;

    @ItemProperty(alias = "处方发起者id,用户标识")
    private String requestMpiId;

    @ItemProperty(alias = "快捷购药处方标识 0 非快捷处方 1 快捷处方")
    private String fastRecipeFlag;

    @ItemProperty(alias = "订单号")
    private String orderCode;

    @ItemProperty(alias = "订单状态")
    private Integer processState;

    @ItemProperty(alias = "退款状态")
    private Integer refundNodeStatus;

    @ItemProperty(alias = "购药方式")
    private String giveModeText;

    @ItemProperty(alias = "供药药企")
    private String name;

    @ItemProperty(alias = "供药药店")
    private String drugStoreName;

    @ItemProperty(alias = "下单人")
    private String requestPatientName;

    @ItemProperty(alias = "下单手机号")
    private String mobile;

    @ItemProperty(alias = "下单时间")
    private Date orderTime;

    @ItemProperty(alias = "支付时间")
    private Date payTime;

    @ItemProperty(alias = "支付流水号")
    private String tradeNo;

    @ItemProperty(alias = "订单金额")
    private BigDecimal totalFee;

    @ItemProperty(alias = "结算类型")
    private String orderTypeText;

    @ItemProperty(alias = "医保金额")
    private Double fundAmount;

    @ItemProperty(alias = "自费金额")
    private Double cashAmount;

    @ItemProperty(alias = "药品费")
    private BigDecimal recipeFee;

    @ItemProperty(alias = "运费")
    private BigDecimal expressFee;

    @ItemProperty(alias = "代煎费")
    private BigDecimal decoctionFee;

    @ItemProperty(alias = "中医辨证论治费")
    private BigDecimal tcmFee;

    @ItemProperty(alias = "药事服务费")
    private BigDecimal auditFee;

    @ItemProperty(alias = "挂号费")
    private BigDecimal registerFee;

    @ItemProperty(alias = "平台处方单号")
    private Integer recipeId;

    @ItemProperty(alias = "HIS处方编码")
    private String recipeCode;

    @ItemProperty(alias = "处方类型")
    private String recipeType;

    @ItemProperty(alias = "开方科室")
    private String appointDepartName;

    @ItemProperty(alias = "开方医生")
    private String doctorName;

    @ItemProperty(alias = "就诊人姓名")
    private String patientName;

    @ItemProperty(alias = "开方时间")
    private Date createDate;

    @ItemProperty(alias = "药品名称")
    private String drugName;

    @ItemProperty(alias = "机构药品ID")
    private String organDrugCode;

    @ItemProperty(alias = "药企药品ID")
    private String saleDrugCode;

    @ItemProperty(alias = "药品单价")
    private BigDecimal salePrice;

    @ItemProperty(alias = "出售数量")
    private Double useTotalDose;

    @ItemProperty(alias = "出售单位")
    private String drugUnit;

    @ItemProperty(alias = "煎法")
    private String decoctionText;

    @ItemProperty(alias = "是否代煎")
    private String generationisOfDecoction;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getFastRecipeFlag() {
        return fastRecipeFlag;
    }

    public void setFastRecipeFlag(String fastRecipeFlag) {
        this.fastRecipeFlag = fastRecipeFlag;
    }

    public Integer getProcessState() {
        return processState;
    }

    public void setProcessState(Integer processState) {
        this.processState = processState;
    }

    public Integer getRefundNodeStatus() {
        return refundNodeStatus;
    }

    public void setRefundNodeStatus(Integer refundNodeStatus) {
        this.refundNodeStatus = refundNodeStatus;
    }

    public String getDecoctionText() {
        return decoctionText;
    }

    public void setDecoctionText(String decoctionText) {
        this.decoctionText = decoctionText;
    }

    public String getGiveModeText() {
        return giveModeText;
    }

    public void setGiveModeText(String giveModeText) {
        this.giveModeText = giveModeText;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Date getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(Date orderTime) {
        this.orderTime = orderTime;
    }

    public Date getPayTime() {
        return payTime;
    }

    public void setPayTime(Date payTime) {
        this.payTime = payTime;
    }

    public String getTradeNo() {
        return tradeNo;
    }

    public void setTradeNo(String tradeNo) {
        this.tradeNo = tradeNo;
    }

    public BigDecimal getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(BigDecimal totalFee) {
        this.totalFee = totalFee;
    }

    public BigDecimal getRecipeFee() {
        return recipeFee;
    }

    public void setRecipeFee(BigDecimal recipeFee) {
        this.recipeFee = recipeFee;
    }

    public BigDecimal getExpressFee() {
        return expressFee;
    }

    public void setExpressFee(BigDecimal expressFee) {
        this.expressFee = expressFee;
    }

    public BigDecimal getDecoctionFee() {
        return decoctionFee;
    }

    public void setDecoctionFee(BigDecimal decoctionFee) {
        this.decoctionFee = decoctionFee;
    }

    public BigDecimal getTcmFee() {
        return tcmFee;
    }

    public void setTcmFee(BigDecimal tcmFee) {
        this.tcmFee = tcmFee;
    }

    public BigDecimal getAuditFee() {
        return auditFee;
    }

    public void setAuditFee(BigDecimal auditFee) {
        this.auditFee = auditFee;
    }

    public BigDecimal getRegisterFee() {
        return registerFee;
    }

    public void setRegisterFee(BigDecimal registerFee) {
        this.registerFee = registerFee;
    }

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

    public String getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(String recipeType) {
        this.recipeType = recipeType;
    }

    public String getAppointDepartName() {
        return appointDepartName;
    }

    public void setAppointDepartName(String appointDepartName) {
        this.appointDepartName = appointDepartName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getOrganDrugCode() {
        return organDrugCode;
    }

    public void setOrganDrugCode(String organDrugCode) {
        this.organDrugCode = organDrugCode;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public Double getUseTotalDose() {
        return useTotalDose;
    }

    public void setUseTotalDose(Double useTotalDose) {
        this.useTotalDose = useTotalDose;
    }

    public String getDrugUnit() {
        return drugUnit;
    }

    public void setDrugUnit(String drugUnit) {
        this.drugUnit = drugUnit;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDrugStoreName() {
        return drugStoreName;
    }

    public void setDrugStoreName(String drugStoreName) {
        this.drugStoreName = drugStoreName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getSaleDrugCode() {
        return saleDrugCode;
    }

    public void setSaleDrugCode(String saleDrugCode) {
        this.saleDrugCode = saleDrugCode;
    }

    public String getGenerationisOfDecoction() {
        return generationisOfDecoction;
    }

    public void setGenerationisOfDecoction(String generationisOfDecoction) {
        this.generationisOfDecoction = generationisOfDecoction;
    }

    public String getRequestMpiId() {
        return requestMpiId;
    }

    public void setRequestMpiId(String requestMpiId) {
        this.requestMpiId = requestMpiId;
    }

    public String getOrderTypeText() {
        return orderTypeText;
    }

    public void setOrderTypeText(String orderTypeText) {
        this.orderTypeText = orderTypeText;
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

    public String getRequestPatientName() {
        return requestPatientName;
    }

    public void setRequestPatientName(String requestPatientName) {
        this.requestPatientName = requestPatientName;
    }
}
