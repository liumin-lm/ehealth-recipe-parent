package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
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

    /**
     * 互联网医院字段
     */
    @ItemProperty(alias = "现病史")
    private String historyOfPresentIllness;

    @ItemProperty(alias = "处理方法")
    private String handleMethod;

    @ItemProperty(alias = "体格检查")
    private String physicalCheck;

    @ItemProperty(alias = "HIS处方关联的卡类型名称")
    private String cardTypeName;

    @ItemProperty(alias = "HIS处方关联的卡类型")
    private String cardType;

    @ItemProperty(alias = "HIS处方关联的卡号")
    private String cardNo;

    @ItemProperty(alias = "医保备案号")
    private String putOnRecordID;

    @ItemProperty(alias = "患者类型 自费 0 商保 1 普通医保 2 慢病医保 3 省医保33 杭州市医保3301 衢州市医保3308 巨化医保3308A")
    private String patientType;
    /**
     * 互联网医院字段
     */

    @ItemProperty(alias = "天猫返回处方编号")
    private String rxNo;

    @ItemProperty(alias = "his返回的取药方式1配送到家 2医院取药 3两者都支持")
    private String giveModeFormHis;

    @ItemProperty(alias = "配送药企代码")
    private String deliveryCode;

    @ItemProperty(alias = "配送药企名称")
    private String deliveryName;

    @ItemProperty(alias = "医保返回的医院机构编码")
    private String hospOrgCodeFromMedical;

    @ItemProperty(alias = "参保地统筹区")
    private String insuredArea;

    @ItemProperty(alias = "医保结算请求串")
    private String medicalSettleData;

    @ItemProperty(alias = "his返回的处方总金额")
    private String deliveryRecipeFee;

    @ItemProperty(alias = "用药医嘱")
    private String drugEntrustment;

    //用户页面选择
    @ItemProperty(alias = "是否长处方")
    private String isLongRecipe;

    //开当前处方的配置项信息
    @ItemProperty(alias = "可开长处方按钮状态、长处方开药天数、非长处方开药天数")
    private String recipeJsonConfig;


    @ItemProperty(alias = "患者医保类型（编码）")
    private String medicalType;


    @ItemProperty(alias = "患者医保类型（名称）")
    private String medicalTypeText;


    @ItemProperty(alias = "第三方处方ID")
    private String rxid;

    @ItemProperty(alias = "制法")
    private String makeMethodId;
    @ItemProperty(alias = "制法text")
    private String makeMethodText;
    @ItemProperty(alias = "每付取汁")
    private String juice;
    @ItemProperty(alias = "每付取汁单位")
    private String juiceUnit;
    @ItemProperty(alias = "次量")
    private String minor;
    @ItemProperty(alias = "次量单位")
    private String minorUnit;
    @ItemProperty(alias = "中医症候编码")
    private String symptomId;
    @ItemProperty(alias = "中医症候名称")
    private String symptomName;
    @ItemProperty(alias = "煎法")
    private String decoctionId;
    @ItemProperty(alias = "煎法text")
    private String decoctionText;
    @ItemProperty(alias = "煎法单价")
    private Double decoctionPrice;

    @ItemProperty(alias = "病历索引Id")
    private Integer docIndexId;

    @ItemProperty(alias = "取药窗口")
    private String pharmNo;

    @ItemProperty(alias = "是否是加急审核处方 0否 1是")
    private Integer canUrgentAuditRecipe;

    @ItemProperty(alias = "诊断序号")
    private String hisDiseaseSerial;

    @ItemProperty(alias = "处方指定药企类型 1医院 2药企 默认 0")
    private Integer appointEnterpriseType;

    @ItemProperty(alias = "是否是儿童处方 0否 1是")
    private Integer childRecipeFlag;

    @Column(name = "appoint_enterprise_type")
    public Integer getAppointEnterpriseType() {
        return appointEnterpriseType;
    }

    public void setAppointEnterpriseType(Integer appointEnterpriseType) {
        this.appointEnterpriseType = appointEnterpriseType;
    }

    @Column(name = "hisDiseaseSerial")
    public String getHisDiseaseSerial() {
        return hisDiseaseSerial;
    }

    public void setHisDiseaseSerial(String hisDiseaseSerial) {
        this.hisDiseaseSerial = hisDiseaseSerial;
    }

    @Column(name = "pharmNo")
    public String getPharmNo() {
        return pharmNo;
    }

    public void setPharmNo(String pharmNo) {
        this.pharmNo = pharmNo;
    }

    public String getSymptomId() {
        return symptomId;
    }

    public void setSymptomId(String symptomId) {
        this.symptomId = symptomId;
    }

    public String getSymptomName() {
        return symptomName;
    }

    public void setSymptomName(String symptomName) {
        this.symptomName = symptomName;
    }

    public String getDecoctionId() {
        return decoctionId;
    }

    public void setDecoctionId(String decoctionId) {
        this.decoctionId = decoctionId;
    }

    public String getDecoctionText() {
        return decoctionText;
    }

    public void setDecoctionText(String decoctionText) {
        this.decoctionText = decoctionText;
    }

    @Column(name = "deliveryRecipeFee")
    public String getDeliveryRecipeFee() {
        return deliveryRecipeFee;
    }

    public void setDeliveryRecipeFee(String deliveryRecipeFee) {
        this.deliveryRecipeFee = deliveryRecipeFee;
    }

    @ItemProperty(alias = "处方来源 0 线下his同步 1 平台处方")
    private Integer fromFlag;

    @ItemProperty(alias = "开处方页面病种选择开关标识")
    private Integer recipeChooseChronicDisease;
    @ItemProperty(alias = "病种标识")
    @Dictionary(id = "eh.cdr.dictionary.ChronicDiseaseFlag")
    private String chronicDiseaseFlag;
    @ItemProperty(alias = "病种代码")
    private String chronicDiseaseCode;
    @ItemProperty(alias = "病种名称")
    private String chronicDiseaseName;
    @ItemProperty(alias = "并发症")
    private String complication;

    @ItemProperty(alias = "电子票号")
    private String einvoiceNumber;

    @ItemProperty(alias = "电子处方监管平台流水号")
    private String superviseRecipecode;

    @ItemProperty(alias = "监管人姓名")
    private String guardianName;
    @ItemProperty(alias = "监管人证件号")
    private String guardianCertificate;
    @ItemProperty(alias = "监管人手机号")
    private String guardianMobile;

    @ItemProperty(alias = "his处方付费序号合集")
    private String recipeCostNumber;

    /**
     * 处方退费当前节点状态。0-待审核；1-审核通过，退款成功；2-审核通过，退款失败；3-审核不通过
     */
    @ItemProperty(alias = "处方退费当前节点状态")
    @Dictionary(id = "eh.cdr.dictionary.RecipeRefundNodeStatus")
    private Integer refundNodeStatus;


    @Column(name = "recipeCostNumber")
    public String getRecipeCostNumber() {
        return recipeCostNumber;
    }

    public void setRecipeCostNumber(String recipeCostNumber) {
        this.recipeCostNumber = recipeCostNumber;
    }

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

    @ItemProperty(alias = "用药说明")
    private String medicationInstruction;

    @ItemProperty(alias = "ca签名ID")
    private String caUniqueID;

    @Column(name = "caUniqueID")
    public String getCaUniqueID() {
        return caUniqueID;
    }

    public void setCaUniqueID(String caUniqueID) {
        this.caUniqueID = caUniqueID;
    }

    @ItemProperty(alias = "药师ca签名ID")
    private String checkCAUniqueID;

    @Column(name = "checkCAUniqueID")
    public String getCheckCAUniqueID() {
        return checkCAUniqueID;
    }

    public void setCheckCAUniqueID(String checkCAUniqueID) {
        this.checkCAUniqueID = checkCAUniqueID;
    }

    @Column(name = "medicationInstruction")
    public String getMedicationInstruction() {
        return medicationInstruction;
    }

    public void setMedicationInstruction(String medicationInstruction) {
        this.medicationInstruction = medicationInstruction;
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
    @Transient
    public String getMainDieaseDescribe() {
        return mainDieaseDescribe;
    }

    public void setMainDieaseDescribe(String mainDieaseDescribe) {
        this.mainDieaseDescribe = mainDieaseDescribe;
    }

    @Column(name = "currentMedical")
    @Transient
    public String getCurrentMedical() {
        return currentMedical;
    }

    public void setCurrentMedical(String currentMedical) {
        this.currentMedical = currentMedical;
    }

    @Column(name = "histroyMedical")
    @Transient
    public String getHistroyMedical() {
        return histroyMedical;
    }

    public void setHistroyMedical(String histroyMedical) {
        this.histroyMedical = histroyMedical;
    }

    @Column(name = "allergyMedical")
    @Transient
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
    @Transient
    public String getHistoryOfPresentIllness() {
        return historyOfPresentIllness;
    }

    public void setHistoryOfPresentIllness(String historyOfPresentIllness) {
        this.historyOfPresentIllness = historyOfPresentIllness;
    }

    @Column(name = "handleMethod")
    @Transient
    public String getHandleMethod() {
        return handleMethod;
    }

    public void setHandleMethod(String handleMethod) {
        this.handleMethod = handleMethod;
    }

    @Column(name = "physicalCheck")
    @Transient
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

    public String getDrugEntrustment() {
        return drugEntrustment;
    }

    public void setDrugEntrustment(String drugEntrustment) {
        this.drugEntrustment = drugEntrustment;
    }

    public String getIsLongRecipe() {
        return isLongRecipe;
    }

    public void setIsLongRecipe(String isLongRecipe) {
        this.isLongRecipe = isLongRecipe;
    }

    public String getRecipeJsonConfig() {
        return recipeJsonConfig;
    }

    public void setRecipeJsonConfig(String recipeJsonConfig) {
        this.recipeJsonConfig = recipeJsonConfig;
    }

    @Column(name = "rxid")
    public String getRxid() {
        return rxid;
    }

    public void setRxid(String rxid) {
        this.rxid = rxid;
    }

    public Integer getRecipeChooseChronicDisease() {
        return recipeChooseChronicDisease;
    }

    public void setRecipeChooseChronicDisease(Integer recipeChooseChronicDisease) {
        this.recipeChooseChronicDisease = recipeChooseChronicDisease;
    }

    public String getComplication() {
        return complication;
    }

    public void setComplication(String complication) {
        this.complication = complication;
    }

    @Column(name = "guardian_name")
    public String getGuardianName() {
        return guardianName;
    }

    public void setGuardianName(String guardianName) {
        this.guardianName = guardianName;
    }

    @Column(name = "guardian_certificate")
    public String getGuardianCertificate() {
        return guardianCertificate;
    }

    public void setGuardianCertificate(String guardianCertificate) {
        this.guardianCertificate = guardianCertificate;
    }

    @Column(name = "guardian_mobile")
    public String getGuardianMobile() {
        return guardianMobile;
    }

    public void setGuardianMobile(String guardianMobile) {
        this.guardianMobile = guardianMobile;
    }

    public String getMakeMethodId() {
        return makeMethodId;
    }

    public void setMakeMethodId(String makeMethodId) {
        this.makeMethodId = makeMethodId;
    }

    public String getMakeMethodText() {
        return makeMethodText;
    }

    public void setMakeMethodText(String makeMethodText) {
        this.makeMethodText = makeMethodText;
    }

    public String getJuice() {
        return juice;
    }

    public void setJuice(String juice) {
        this.juice = juice;
    }

    public String getJuiceUnit() {
        return juiceUnit;
    }

    public void setJuiceUnit(String juiceUnit) {
        this.juiceUnit = juiceUnit;
    }

    public String getMinor() {
        return minor;
    }

    public void setMinor(String minor) {
        this.minor = minor;
    }

    public String getMinorUnit() {
        return minorUnit;
    }

    public void setMinorUnit(String minorUnit) {
        this.minorUnit = minorUnit;
    }

    @Transient
    public Double getDecoctionPrice() {
        return decoctionPrice;
    }

    public void setDecoctionPrice(Double decoctionPrice) {
        this.decoctionPrice = decoctionPrice;
    }

    @Column(name = "docIndexId")
    public Integer getDocIndexId() {
        return docIndexId;
    }

    public void setDocIndexId(Integer docIndexId) {
        this.docIndexId = docIndexId;
    }

    /**
     * 处方退费当前节点状态。0-待审核；1-审核通过，退款成功；2-审核通过，退款失败；3-审核不通过
     */
    @Column(name = "refundNodeStatus")
    public Integer getRefundNodeStatus() {
        return refundNodeStatus;
    }

    public void setRefundNodeStatus(Integer refundNodeStatus) {
        this.refundNodeStatus = refundNodeStatus;
    }

    /**
     * 加急处方
     *
     * @return
     */
    @Column(name = "canUrgentAuditRecipe")
    public Integer getCanUrgentAuditRecipe() {
        return canUrgentAuditRecipe;
    }

    public void setCanUrgentAuditRecipe(Integer canUrgentAuditRecipe) {
        this.canUrgentAuditRecipe = canUrgentAuditRecipe;
    }

    public Integer getChildRecipeFlag() {
        return childRecipeFlag;
    }

    public void setChildRecipeFlag(Integer childRecipeFlag) {
        this.childRecipeFlag = childRecipeFlag;
    }
}
