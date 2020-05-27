package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * cdr_recipe扩展表
 */
@Entity
@Schema
@Table(name = "cdr_recipe_ext")
@Access(AccessType.PROPERTY)
public class RecipeExtend implements Serializable {

    private static final long serialVersionUID = -7396436464542532302L;
    
    @ItemProperty(alias = "处方ID")
    private Integer recipeId;

    @ItemProperty(alias = "主诉")
    private String mainDieaseDescribe;

    @ItemProperty(alias = "现病史")
    private String currentMedical;

    @ItemProperty(alias = "既往史")
    private String histroyMedical;

    @ItemProperty(alias = "过敏史")
    private String allergyMedical;

    @ItemProperty(alias = "挂号序号")
    private String registerID;

    @ItemProperty(alias = "发病日期")
    private Date onsetDate;

    /**互联网医院字段*/
    @ItemProperty(alias = "现病史")
    private String historyOfPresentIllness;

    @ItemProperty(alias = "处理方法")
    private String handleMethod;

    @ItemProperty(alias = "体格检查")
    private String physicalCheck;

    @ItemProperty(alias = "HIS处方关联的卡类型名称")
    private String  cardTypeName;

    @ItemProperty(alias = "HIS处方关联的卡类型")
    private String  cardType;

    @ItemProperty(alias = "HIS处方关联的卡号")
    private String  cardNo;

    @ItemProperty(alias = "医保备案号")
    private String putOnRecordID;

    @ItemProperty(alias = "患者类型 自费 0 商保 1 普通医保 2 慢病医保 3 省医保33 杭州市医保3301 衢州市医保3308 巨化医保3308A")
    private String patientType;
    /**互联网医院字段*/

    @ItemProperty(alias = "天猫返回处方编号")
    private String  rxNo;

    @ItemProperty(alias = "his返回的取药方式1配送到家 2医院取药 3两者都支持")
    private String giveModeFormHis;

    @ItemProperty(alias = "his返回的配送药企代码")
    private String deliveryCode;

    @ItemProperty(alias = "his返回的配送药企名称")
    private String deliveryName;

    @ItemProperty(alias = "医保返回的医院机构编码")
    private String hospOrgCodeFromMedical;

    @ItemProperty(alias = "参保地统筹区")
    private String insuredArea;

    @ItemProperty(alias = "医保结算请求串")
    private String medicalSettleData;

    @ItemProperty(alias = "门诊挂号序号（医保）")
    private String registerNo;

    @ItemProperty(alias = "HIS收据号（医保）")
    private String hisSettlementNo;

    @ItemProperty(alias = "处方预结算返回支付总金额")
    private String preSettleTotalAmount;

    @ItemProperty(alias = "处方预结算返回医保支付金额")
    private String fundAmount;

    @ItemProperty(alias = "处方预结算返回自费金额")
    private String cashAmount;

    @ItemProperty(alias = "his返回的处方总金额")
    private String deliveryRecipeFee;

    /**
     * 患者医保类型（编码）
     */
    private String medicalType;

    /**
     * 患者医保类型（名称）
     */
    private String medicalTypeText;

//    @ItemProperty(alias = "处方创建状态  0：未进行，1：已生成订单 ，2.已创建处方，3. 已预支付处方")
//    private String orderMakeStatus;
//
//    @ItemProperty(alias = "处方")
//    private String deliverySendTag;

//    public String getOrderMakeStatus() {
//        return orderMakeStatus;
//    }
//
//    public void setOrderMakeStatus(String orderMakeStatus) {
//        this.orderMakeStatus = orderMakeStatus;
//    }

    @Column(name = "deliveryRecipeFee")
    public String getDeliveryRecipeFee() {
        return deliveryRecipeFee;
    }

    public void setDeliveryRecipeFee(String deliveryRecipeFee) {
        this.deliveryRecipeFee = deliveryRecipeFee;
    }

    @ItemProperty(alias = "处方预结算返回应付金额")
    private String payAmount;

    @ItemProperty(alias = "处方来源 0 线下his同步 1 平台处方")
    private Integer fromFlag;

    @ItemProperty(alias = "慢病病种标识")
    private String chronicDiseaseFlag;
    @ItemProperty(alias = "慢病病种代码")
    private String chronicDiseaseCode;
    @ItemProperty(alias = "慢病病种名称")
    private String chronicDiseaseName;

    @ItemProperty(alias = "电子票号")
    private String einvoiceNumber;

    @ItemProperty(alias = "电子处方监管平台流水号")
    private String superviseRecipecode;

    @Column(name = "superviseRecipecode")
    public String getSuperviseRecipecode() {
        return superviseRecipecode;
    }

    public void setSuperviseRecipecode(String superviseRecipecode) {
        this.superviseRecipecode = superviseRecipecode;
    }

    @Column(name = "einvoiceNumber")
    public String getEinvoiceNumber() {
        return einvoiceNumber;
    }

    public void setEinvoiceNumber(String einvoiceNumber) {
        this.einvoiceNumber = einvoiceNumber;
    }

    public RecipeExtend() {
    }

    public RecipeExtend(Integer recipeId, String historyOfPresentIllness,
                        String mainDieaseDescribe, String handleMethod, String physicalCheck) {
        this.recipeId = recipeId;
        this.historyOfPresentIllness = historyOfPresentIllness;
        this.mainDieaseDescribe = mainDieaseDescribe;
        this.handleMethod = handleMethod;
        this.physicalCheck = physicalCheck;
    }

    @Id
    @Column(name = "recipeId", unique = true, nullable = false)
    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    @Column(name = "mainDieaseDescribe")
    public String getMainDieaseDescribe() {
        return mainDieaseDescribe;
    }

    public void setMainDieaseDescribe(String mainDieaseDescribe) {
        this.mainDieaseDescribe = mainDieaseDescribe;
    }

    @Column(name = "currentMedical")
    public String getCurrentMedical() {
        return currentMedical;
    }

    public void setCurrentMedical(String currentMedical) {
        this.currentMedical = currentMedical;
    }

    @Column(name = "histroyMedical")
    public String getHistroyMedical() {
        return histroyMedical;
    }

    public void setHistroyMedical(String histroyMedical) {
        this.histroyMedical = histroyMedical;
    }

    @Column(name = "allergyMedical")
    public String getAllergyMedical() {
        return allergyMedical;
    }

    public void setAllergyMedical(String allergyMedical) {
        this.allergyMedical = allergyMedical;
    }

    @Column(name = "onsetDate")
    public Date getOnsetDate() {
        return onsetDate;
    }

    public void setOnsetDate(Date onsetDate) {
        this.onsetDate = onsetDate;
    }

    @Column(name = "historyOfPresentIllness")
    public String getHistoryOfPresentIllness() {
        return historyOfPresentIllness;
    }

    public void setHistoryOfPresentIllness(String historyOfPresentIllness) {
        this.historyOfPresentIllness = historyOfPresentIllness;
    }

    @Column(name = "handleMethod")
    public String getHandleMethod() {
        return handleMethod;
    }

    public void setHandleMethod(String handleMethod) {
        this.handleMethod = handleMethod;
    }

    @Column(name = "physicalCheck")
    public String getPhysicalCheck() {
        return physicalCheck;
    }

    public void setPhysicalCheck(String physicalCheck) {
        this.physicalCheck = physicalCheck;
    }

    @Column(name = "cardTypeName")
    public String getCardTypeName() {
        return cardTypeName;
    }

    public void setCardTypeName(String cardTypeName) {
        this.cardTypeName = cardTypeName;
    }

    @Column(name = "cardNo")
    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }


    @Column(name = "registerID")
    public String getRegisterID() {
        return registerID;
    }

    public void setRegisterID(String registerID) {
        this.registerID = registerID;
    }

    @Column(name = "rxNo")
    public String getRxNo() {
        return rxNo;
    }

    public void setRxNo(String rxNo) {
        this.rxNo = rxNo;
    }

    @Column(name = "giveModeFormHis")
    public String getGiveModeFormHis() {
        return giveModeFormHis;
    }

    public void setGiveModeFormHis(String giveModeFormHis) {
        this.giveModeFormHis = giveModeFormHis;
    }

    @Column(name = "deliveryCode")
    public String getDeliveryCode() {
        return deliveryCode;
    }

    public void setDeliveryCode(String deliveryCode) {
        this.deliveryCode = deliveryCode;
    }

    @Column(name = "deliveryName")
    public String getDeliveryName() {
        return deliveryName;
    }

    public void setDeliveryName(String deliveryName) {
        this.deliveryName = deliveryName;
    }

    @Column(name = "insuredArea")
    public String getInsuredArea() {
        return insuredArea;
    }

    public void setInsuredArea(String insuredArea) {
        this.insuredArea = insuredArea;
    }

    @Column(name = "medicalSettleData")
    public String getMedicalSettleData() {
        return medicalSettleData;
    }

    public void setMedicalSettleData(String medicalSettleData) {
        this.medicalSettleData = medicalSettleData;
    }

    @Column(name = "hospOrgCodeFromMedical")
    public String getHospOrgCodeFromMedical() {
        return hospOrgCodeFromMedical;
    }

    public void setHospOrgCodeFromMedical(String hospOrgCodeFromMedical) {
        this.hospOrgCodeFromMedical = hospOrgCodeFromMedical;
    }

    @Column(name = "putOnRecordID")
    public String getPutOnRecordID() {
        return putOnRecordID;
    }

    public void setPutOnRecordID(String putOnRecordID) {
        this.putOnRecordID = putOnRecordID;
    }

    @Column(name = "patientType")
    public String getPatientType() {
        return patientType;
    }

    public void setPatientType(String patientType) {
        this.patientType = patientType;
    }

    @Column(name = "registerNo")
    public String getRegisterNo() {
        return registerNo;
    }

    public void setRegisterNo(String registerNo) {
        this.registerNo = registerNo;
    }

    @Column(name = "hisSettlementNo")
    public String getHisSettlementNo() {
        return hisSettlementNo;
    }

    public void setHisSettlementNo(String hisSettlementNo) {
        this.hisSettlementNo = hisSettlementNo;
    }

    @Column(name = "preSettleTotalAmount")
    public String getPreSettletotalAmount() {
        return preSettleTotalAmount;
    }

    public void setPreSettletotalAmount(String preSettleTotalAmount) {
        this.preSettleTotalAmount = preSettleTotalAmount;
    }

    @Column(name = "fundAmount")
    public String getFundAmount() {
        return fundAmount;
    }

    public void setFundAmount(String fundAmount) {
        this.fundAmount = fundAmount;
    }

    @Column(name = "cashAmount")
    public String getCashAmount() {
        return cashAmount;
    }

    public void setCashAmount(String cashAmount) {
        this.cashAmount = cashAmount;
    }

    @Column(name = "payAmount")
    public String getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(String payAmount) {
        this.payAmount = payAmount;
    }

    @Column(name = "fromFlag")
    public Integer getFromFlag() {
        return fromFlag;
    }

    public void setFromFlag(Integer fromFlag) {
        this.fromFlag = fromFlag;
    }

    @Column(name = "cardType")
    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    @Column(name = "chronicDiseaseFlag")
    public String getChronicDiseaseFlag() {
        return chronicDiseaseFlag;
    }

    public void setChronicDiseaseFlag(String chronicDiseaseFlag) {
        this.chronicDiseaseFlag = chronicDiseaseFlag;
    }

    @Column(name = "chronicDiseaseCode")
    public String getChronicDiseaseCode() {
        return chronicDiseaseCode;
    }

    public void setChronicDiseaseCode(String chronicDiseaseCode) {
        this.chronicDiseaseCode = chronicDiseaseCode;
    }

    @Column(name = "chronicDiseaseName")
    public String getChronicDiseaseName() {
        return chronicDiseaseName;
    }

    public void setChronicDiseaseName(String chronicDiseaseName) {
        this.chronicDiseaseName = chronicDiseaseName;
    }

    public String getMedicalType() {
        return medicalType;
    }

    public void setMedicalType(String medicalType) {
        this.medicalType = medicalType;
    }

    public String getMedicalTypeText() {
        return medicalTypeText;
    }

    public void setMedicalTypeText(String medicalTypeText) {
        this.medicalTypeText = medicalTypeText;
    }
}
