package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.*;

import java.io.Serializable;
import java.util.Date;

/**
 * 处方扩展信息
 */
@Schema
public class RecipeExtendBean implements Serializable {

    private static final long serialVersionUID = 2528413275115207345L;

    @ItemProperty(alias = "处方ID")
    private Integer recipeId;

    @ItemProperty(alias = "挂号序号")
    private String registerID;

    @ItemProperty(alias = "主诉")
    private String mainDieaseDescribe;

    @ItemProperty(alias = "现病史")
    private String currentMedical;

    @ItemProperty(alias = "既往史")
    private String histroyMedical;

    @ItemProperty(alias = "过敏史")
    private String allergyMedical;

    @ItemProperty(alias = "发病日期")
    private Date onsetDate;

    /**
     * 以下为互联网医院字段
     */
    @ItemProperty(alias = "现病史")
    private String historyOfPresentIllness;

    @ItemProperty(alias = "处理方法")
    private String handleMethod;

    @ItemProperty(alias = "体格检查")
    private String physicalCheck;
    /**
     * 为互联网医院字段
     */
    @ItemProperty(alias = "HIS处方关联的卡类型")
    private String cardTypeName;

    @ItemProperty(alias = "HIS处方关联的卡号")
    @Desensitizations(type = DesensitizationsType.HEALTHCARD)
    private String cardNo;

    @ItemProperty(alias = "HIS处方关联的卡类型")
    private String cardType;

    @ItemProperty(alias = "患者类型 自费 0 商保 1 普通医保 2 慢病医保 3 省医保33 杭州市医保3301 衢州市医保3308 巨化医保3308A")
    private String patientType;

    @ItemProperty(alias = "配送药企代码")
    private String deliveryCode;

    @ItemProperty(alias = "配送药企名称")
    private String deliveryName;
    @ItemProperty(alias = "处方指定药企类型 1医院 2药企 默认 0")
    private Integer appointEnterpriseType;

    @ItemProperty(alias = "医保返回的医院机构编码")
    private String hospOrgCodeFromMedical;

    @ItemProperty(alias = "参保地统筹区")
    private String insuredArea;

    @ItemProperty(alias = "医保结算请求串")
    private String medicalSettleData;

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
    @ItemProperty(alias = "用药医嘱（药师审方）")
    private String drugEntrustment;
    //用户页面选择
    @ItemProperty(alias = "是否长处方")
    private String isLongRecipe;

    //开当前处方的配置项信息
    @ItemProperty(alias = "可开长处方按钮状态、长处方开药天数、非长处方开药天数")
    private String recipeJsonConfig;

    @ItemProperty(alias = "监管人姓名")
    @Desensitizations(type = DesensitizationsType.NAME)
    private String guardianName;
    @ItemProperty(alias = "监管人证件号")
    @Desensitizations(type = DesensitizationsType.IDCARD)
    private String guardianCertificate;
    @ItemProperty(alias = "监管人手机号")
    @Desensitizations(type = DesensitizationsType.MOBILE)
    private String guardianMobile;

    /**
     * 患者医保类型（编码）
     */
    private String medicalType;

    /**
     * 患者医保类型（名称）
     */
    private String medicalTypeText;

    @ItemProperty(alias = "制法")
    private String makeMethodId;
    @ItemProperty(alias = "制法text")
    private String makeMethodText;
    @ItemProperty(alias = "每贴次数")
    private String everyTcmNumFre;
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
    @ItemProperty(alias = "服用要求")
    private String requirementsForTakingId;
    @ItemProperty(alias = "服用要求code")
    private String requirementsForTakingCode;
    @ItemProperty(alias = "服用要求text")
    private String requirementsForTakingText;

    @ItemProperty(alias = "制法编码")
    private String makeMethod;
    @ItemProperty(alias = "中医症候编码")
    private String symptomCode;
    @ItemProperty(alias = "煎法编码")
    private String decoctionCode;
    @ItemProperty(alias = "煎法单价")
    private Double decoctionPrice;

    @ItemProperty(alias = "病历索引Id")
    private Integer docIndexId;

    @ItemProperty(alias = "用药说明")
    private String medicationInstruction;

    @ItemProperty(alias = "ca签名ID")
    private String caUniqueID;
    @ItemProperty(alias = "药师ca签名ID")
    private String checkCAUniqueID;

    @ItemProperty(alias = "his返回的处方总金额")
    private String deliveryRecipeFee;

    @ItemProperty(alias = "his处方付费序号合集")
    private String recipeCostNumber;

    @ItemProperty(alias = "收费项编码")
    private String chargeItemCode;

    //date 20201013 字段补录,同步recipeExtend字段
    @ItemProperty(alias = "医保备案号")
    private String putOnRecordID;

    @ItemProperty(alias = "天猫返回处方编号")
    private String rxNo;

    @ItemProperty(alias = "his返回的取药方式1配送到家 2医院取药 3两者都支持")
    private String giveModeFormHis;

    @ItemProperty(alias = "皮肤反应测验")
    private String skinTest;

    @ItemProperty(alias = "处方来源 0 线下his同步 1 平台处方")
    private Integer fromFlag;

    @ItemProperty(alias = "取药窗口")
    private String pharmNo;

    @ItemProperty(alias = "是否是加急审核处方 0否 1是")
    private Integer canUrgentAuditRecipe;

    @ItemProperty(alias = "电子处方监管平台流水号")
    private String superviseRecipecode;

    @ItemProperty(alias = "处方标识 0:普通处方 1:儿童处方")
    private Integer recipeFlag;

    @ItemProperty(alias = "撤销原因")
    private String cancellation;

    @ItemProperty(alias = "强制自费的标识 1 强制 2 不强制")
    private Integer forceCashType;

    /**
     * 处方退费当前节点状态。0-待审核；1-审核通过，退款成功；2-审核通过，退款失败；3-审核不通过
     */
    @ItemProperty(alias = "处方退费当前节点状态")
    @Dictionary(id = "eh.cdr.dictionary.RecipeRefundNodeStatus")
    private Integer refundNodeStatus;

    @ItemProperty(alias = "处方业务类型  1 门诊处方  2  复诊处方  3 其他处方")
    private Integer recipeBusinessType;

    private String recipeBusinessText;

    //用户页面选择
    @ItemProperty(alias = "医生选择是否代煎（ 0:否 1：是）")
    private String doctorIsDecoction;

    @ItemProperty(alias = "代煎前端展示 0 不展示 1 展示")
    private Integer decoctionExhibitionFlag;

    @ItemProperty(alias = "病历号")
    private String medicalRecordNumber;

    @ItemProperty(alias = "大病类型")
    private String illnessType;
    @ItemProperty(alias = "大病类型名称")
    private String illnessName;

    @ItemProperty(alias = "业务办理类型 2父母 3子女 4自愿者代办")
    private String handleType;

    @ItemProperty(alias = "终端ID")
    private String terminalId;

    @ItemProperty(alias = "终端类型 1 自助机")
    private Integer terminalType;

    @ItemProperty(alias = "是否是自助机")
    private Boolean selfServiceMachineFlag;

    @ItemProperty(alias = "电子票号")
    private String einvoiceNumber;

    @ItemProperty(alias = "药方单Id")
    private Integer mouldId;

    @ItemProperty(alias = "快捷购药购买份数")
    private Integer fastRecipeNum;

    @ItemProperty(alias = "快捷购药分享医生")
    private Integer fastRecipeShareDoctor;

    @ItemProperty(alias = "快捷购药分享医生名称")
    private String fastRecipeShareDoctorName;

    @ItemProperty(alias = "快捷购药分享科室代码")
    private String fastRecipeShareDepart;

    @ItemProperty(alias = "快捷购药分享科室名称")
    private String fastRecipeShareDepartName;

    /**
     * his订单编号(邵逸夫)
     */
    private String hisOrderCode;

    @ItemProperty(alias = "是否自动审核 1自动审核，0/null药师审核")
    private Integer autoCheck;

    @ItemProperty(alias = "单复方表示0:无状态，1单方，2复方")
    private Integer singleOrCompoundRecipe;

    public Integer getSingleOrCompoundRecipe() {
        return singleOrCompoundRecipe;
    }

    public void setSingleOrCompoundRecipe(Integer singleOrCompoundRecipe) {
        this.singleOrCompoundRecipe = singleOrCompoundRecipe;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public Integer getTerminalType() {
        return terminalType;
    }

    public void setTerminalType(Integer terminalType) {
        this.terminalType = terminalType;
    }

    public Boolean isSelfServiceMachineFlag() {
        return selfServiceMachineFlag;
    }

    public void setSelfServiceMachineFlag(Boolean selfServiceMachineFlag) {
        this.selfServiceMachineFlag = selfServiceMachineFlag;
    }

    public String getIllnessName() {
        return illnessName;
    }

    public void setIllnessName(String illnessName) {
        this.illnessName = illnessName;
    }

    public String getIllnessType() {
        return illnessType;
    }

    public void setIllnessType(String illnessType) {
        this.illnessType = illnessType;
    }

    public String getDoctorIsDecoction() {
        return doctorIsDecoction;
    }

    public void setDoctorIsDecoction(String doctorIsDecoction) {
        this.doctorIsDecoction = doctorIsDecoction;
    }

    public Integer getAppointEnterpriseType() {
        return appointEnterpriseType;
    }

    public void setAppointEnterpriseType(Integer appointEnterpriseType) {
        this.appointEnterpriseType = appointEnterpriseType;
    }

    public String getRequirementsForTakingId() {
        return requirementsForTakingId;
    }

    public void setRequirementsForTakingId(String requirementsForTakingId) {
        this.requirementsForTakingId = requirementsForTakingId;
    }

    public String getRequirementsForTakingCode() {
        return requirementsForTakingCode;
    }

    public void setRequirementsForTakingCode(String requirementsForTakingCode) {
        this.requirementsForTakingCode = requirementsForTakingCode;
    }

    public String getRequirementsForTakingText() {
        return requirementsForTakingText;
    }

    public void setRequirementsForTakingText(String requirementsForTakingText) {
        this.requirementsForTakingText = requirementsForTakingText;
    }

    public String getPharmNo() {
        return pharmNo;
    }

    public void setPharmNo(String pharmNo) {
        this.pharmNo = pharmNo;
    }

    public String getPutOnRecordID() {
        return putOnRecordID;
    }

    public void setPutOnRecordID(String putOnRecordID) {
        this.putOnRecordID = putOnRecordID;
    }

    public String getRxNo() {
        return rxNo;
    }

    public void setRxNo(String rxNo) {
        this.rxNo = rxNo;
    }

    public String getGiveModeFormHis() {
        return giveModeFormHis;
    }

    public void setGiveModeFormHis(String giveModeFormHis) {
        this.giveModeFormHis = giveModeFormHis;
    }

    public String getSkinTest() {
        return skinTest;
    }

    public void setSkinTest(String skinTest) {
        this.skinTest = skinTest;
    }

    public Integer getFromFlag() {
        return fromFlag;
    }

    public void setFromFlag(Integer fromFlag) {
        this.fromFlag = fromFlag;
    }

    public String getRecipeCostNumber() {
        return recipeCostNumber;
    }

    public void setRecipeCostNumber(String recipeCostNumber) {
        this.recipeCostNumber = recipeCostNumber;
    }

    public String getDeliveryRecipeFee() {
        return deliveryRecipeFee;
    }

    public void setDeliveryRecipeFee(String deliveryRecipeFee) {
        this.deliveryRecipeFee = deliveryRecipeFee;
    }

    public String getCheckCAUniqueID() {
        return checkCAUniqueID;
    }

    public void setCheckCAUniqueID(String checkCAUniqueID) {
        this.checkCAUniqueID = checkCAUniqueID;
    }

    public String getCaUniqueID() {
        return caUniqueID;
    }

    public void setCaUniqueID(String caUniqueID) {
        this.caUniqueID = caUniqueID;
    }

    public String getMedicationInstruction() {
        return medicationInstruction;
    }

    public void setMedicationInstruction(String medicationInstruction) {
        this.medicationInstruction = medicationInstruction;
    }

    public String getMakeMethod() {
        return makeMethod;
    }

    public void setMakeMethod(String makeMethod) {
        this.makeMethod = makeMethod;
    }

    public String getSymptomCode() {
        return symptomCode;
    }

    public void setSymptomCode(String symptomCode) {
        this.symptomCode = symptomCode;
    }

    public String getDecoctionCode() {
        return decoctionCode;
    }

    public void setDecoctionCode(String decoctionCode) {
        this.decoctionCode = decoctionCode;
    }

    public String getDecoctionText() {
        return decoctionText;
    }

    public void setDecoctionText(String decoctionText) {
        this.decoctionText = decoctionText;
    }

    public String getSymptomId() {
        return symptomId;
    }

    public void setSymptomId(String symptomId) {
        this.symptomId = symptomId;
    }

    public String getDecoctionId() {
        return decoctionId;
    }

    public void setDecoctionId(String decoctionId) {
        this.decoctionId = decoctionId;
    }

    public String getSymptomName() {
        return symptomName;
    }

    public void setSymptomName(String symptomName) {
        this.symptomName = symptomName;
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

    public RecipeExtendBean() {
    }

    public RecipeExtendBean(Integer recipeId, String historyOfPresentIllness, String mainDieaseDescribe,
                            String handleMethod, String physicalCheck) {
        this.recipeId = recipeId;
        this.historyOfPresentIllness = historyOfPresentIllness;
        this.mainDieaseDescribe = mainDieaseDescribe;
        this.handleMethod = handleMethod;
        this.physicalCheck = physicalCheck;
    }

    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public String getMainDieaseDescribe() {
        return mainDieaseDescribe;
    }

    public void setMainDieaseDescribe(String mainDieaseDescribe) {
        this.mainDieaseDescribe = mainDieaseDescribe;
    }

    public String getCurrentMedical() {
        return currentMedical;
    }

    public void setCurrentMedical(String currentMedical) {
        this.currentMedical = currentMedical;
    }

    public String getHistroyMedical() {
        return histroyMedical;
    }

    public void setHistroyMedical(String histroyMedical) {
        this.histroyMedical = histroyMedical;
    }

    public String getAllergyMedical() {
        return allergyMedical;
    }

    public void setAllergyMedical(String allergyMedical) {
        this.allergyMedical = allergyMedical;
    }

    public Date getOnsetDate() {
        return onsetDate;
    }

    public void setOnsetDate(Date onsetDate) {
        this.onsetDate = onsetDate;
    }

    public String getHistoryOfPresentIllness() {
        return historyOfPresentIllness;
    }

    public void setHistoryOfPresentIllness(String historyOfPresentIllness) {
        this.historyOfPresentIllness = historyOfPresentIllness;
    }

    public String getHandleMethod() {
        return handleMethod;
    }

    public void setHandleMethod(String handleMethod) {
        this.handleMethod = handleMethod;
    }

    public String getPhysicalCheck() {
        return physicalCheck;
    }

    public void setPhysicalCheck(String physicalCheck) {
        this.physicalCheck = physicalCheck;
    }

    public String getCardTypeName() {
        return cardTypeName;
    }

    public void setCardTypeName(String cardTypeName) {
        this.cardTypeName = cardTypeName;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public String getRegisterID() {
        return registerID;
    }

    public void setRegisterID(String registerID) {
        this.registerID = registerID;
    }

    public String getDeliveryCode() {
        return deliveryCode;
    }

    public void setDeliveryCode(String deliveryCode) {
        this.deliveryCode = deliveryCode;
    }

    public String getDeliveryName() {
        return deliveryName;
    }

    public void setDeliveryName(String deliveryName) {
        this.deliveryName = deliveryName;
    }

    public String getHospOrgCodeFromMedical() {
        return hospOrgCodeFromMedical;
    }

    public void setHospOrgCodeFromMedical(String hospOrgCodeFromMedical) {
        this.hospOrgCodeFromMedical = hospOrgCodeFromMedical;
    }

    public String getInsuredArea() {
        return insuredArea;
    }

    public void setInsuredArea(String insuredArea) {
        this.insuredArea = insuredArea;
    }

    public String getMedicalSettleData() {
        return medicalSettleData;
    }

    public void setMedicalSettleData(String medicalSettleData) {
        this.medicalSettleData = medicalSettleData;
    }

    public String getChronicDiseaseFlag() {
        return chronicDiseaseFlag;
    }

    public void setChronicDiseaseFlag(String chronicDiseaseFlag) {
        this.chronicDiseaseFlag = chronicDiseaseFlag;
    }

    public String getChronicDiseaseCode() {
        return chronicDiseaseCode;
    }

    public void setChronicDiseaseCode(String chronicDiseaseCode) {
        this.chronicDiseaseCode = chronicDiseaseCode;
    }

    public String getChronicDiseaseName() {
        return chronicDiseaseName;
    }

    public void setChronicDiseaseName(String chronicDiseaseName) {
        this.chronicDiseaseName = chronicDiseaseName;
    }

    public String getPatientType() {
        return patientType;
    }

    public void setPatientType(String patientType) {
        this.patientType = patientType;
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

    public String getGuardianName() {
        return guardianName;
    }

    public void setGuardianName(String guardianName) {
        this.guardianName = guardianName;
    }

    public String getGuardianCertificate() {
        return guardianCertificate;
    }

    public void setGuardianCertificate(String guardianCertificate) {
        this.guardianCertificate = guardianCertificate;
    }

    public String getGuardianMobile() {
        return guardianMobile;
    }

    public void setGuardianMobile(String guardianMobile) {
        this.guardianMobile = guardianMobile;
    }

    public Integer getDocIndexId() {
        return docIndexId;
    }

    public void setDocIndexId(Integer docIndexId) {
        this.docIndexId = docIndexId;
    }

    public String getEveryTcmNumFre() {
        return everyTcmNumFre;
    }

    public void setEveryTcmNumFre(String everyTcmNumFre) {
        this.everyTcmNumFre = everyTcmNumFre;
    }

    public Double getDecoctionPrice() {
        return decoctionPrice;
    }

    public void setDecoctionPrice(Double decoctionPrice) {
        this.decoctionPrice = decoctionPrice;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public Integer getCanUrgentAuditRecipe() {
        return canUrgentAuditRecipe;
    }

    public void setCanUrgentAuditRecipe(Integer canUrgentAuditRecipe) {
        this.canUrgentAuditRecipe = canUrgentAuditRecipe;
    }

    public String getSuperviseRecipecode() {
        return superviseRecipecode;
    }

    public void setSuperviseRecipecode(String superviseRecipecode) {
        this.superviseRecipecode = superviseRecipecode;
    }

    public Integer getRecipeFlag() {
        return recipeFlag;
    }

    public void setRecipeFlag(Integer recipeFlag) {
        this.recipeFlag = recipeFlag;
    }

    public String getCancellation() {
        return cancellation;
    }

    public void setCancellation(String cancellation) {
        this.cancellation = cancellation;
    }

    public Integer getForceCashType() {
        return forceCashType;
    }

    public void setForceCashType(Integer forceCashType) {
        this.forceCashType = forceCashType;
    }

    public Integer getRecipeBusinessType() {
        return recipeBusinessType;
    }

    public void setRecipeBusinessType(Integer recipeBusinessType) {
        this.recipeBusinessType = recipeBusinessType;
    }

    public String getMedicalRecordNumber() {
        return medicalRecordNumber;
    }

    public void setMedicalRecordNumber(String medicalRecordNumber) {
        this.medicalRecordNumber = medicalRecordNumber;
    }

    public String getHandleType() {
        return handleType;
    }

    public void setHandleType(String handleType) {
        this.handleType = handleType;
    }

    public String getRecipeBusinessText() {
        return recipeBusinessText;
    }

    public void setRecipeBusinessText(String recipeBusinessText) {
        this.recipeBusinessText = recipeBusinessText;
    }

    public Integer getRefundNodeStatus() {
        return refundNodeStatus;
    }

    public void setRefundNodeStatus(Integer refundNodeStatus) {
        this.refundNodeStatus = refundNodeStatus;
    }

    public String getEinvoiceNumber() {
        return einvoiceNumber;
    }

    public void setEinvoiceNumber(String einvoiceNumber) {
        this.einvoiceNumber = einvoiceNumber;
    }


    public Boolean getSelfServiceMachineFlag() {
        return selfServiceMachineFlag;
    }

    public String getHisOrderCode() {
        return hisOrderCode;
    }

    public void setHisOrderCode(String hisOrderCode) {
        this.hisOrderCode = hisOrderCode;
    }

    public Integer getAutoCheck() {
        return autoCheck;
    }

    public void setAutoCheck(Integer autoCheck) {
        this.autoCheck = autoCheck;

    }

    public String getChargeItemCode() {
        return chargeItemCode;
    }

    public void setChargeItemCode(String chargeItemCode) {
        this.chargeItemCode = chargeItemCode;
    }

    public Integer getMouldId() {
        return mouldId;
    }

    public void setMouldId(Integer mouldId) {
        this.mouldId = mouldId;
    }

    public Integer getFastRecipeNum() {
        return fastRecipeNum;
    }

    public void setFastRecipeNum(Integer fastRecipeNum) {
        this.fastRecipeNum = fastRecipeNum;
    }

    public Integer getDecoctionExhibitionFlag() {
        return decoctionExhibitionFlag;
    }

    public void setDecoctionExhibitionFlag(Integer decoctionExhibitionFlag) {
        this.decoctionExhibitionFlag = decoctionExhibitionFlag;
    }

    public Integer getFastRecipeShareDoctor() {
        return fastRecipeShareDoctor;
    }

    public void setFastRecipeShareDoctor(Integer fastRecipeShareDoctor) {
        this.fastRecipeShareDoctor = fastRecipeShareDoctor;
    }

    public String getFastRecipeShareDoctorName() {
        return fastRecipeShareDoctorName;
    }

    public void setFastRecipeShareDoctorName(String fastRecipeShareDoctorName) {
        this.fastRecipeShareDoctorName = fastRecipeShareDoctorName;
    }

    public String getFastRecipeShareDepart() {
        return fastRecipeShareDepart;
    }

    public void setFastRecipeShareDepart(String fastRecipeShareDepart) {
        this.fastRecipeShareDepart = fastRecipeShareDepart;
    }

    public String getFastRecipeShareDepartName() {
        return fastRecipeShareDepartName;
    }

    public void setFastRecipeShareDepartName(String fastRecipeShareDepartName) {
        this.fastRecipeShareDepartName = fastRecipeShareDepartName;
    }
}
