package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author yinsheng
 * @date 2020\3\10 0010 17:14
 */
@Entity
@Schema
@Table(name = "cdr_his_recipe")
@Access(AccessType.PROPERTY)
public class HisRecipe implements Serializable {
    private static final long serialVersionUID = -7402055250734266918L;

    @ItemProperty(alias = "处方序号")
    private Integer hisRecipeID; // int(11) NOT NULL AUTO_INCREMENT,
    @ItemProperty(alias = "挂号序号")
    private String registeredId; // varchar(30) DEFAULT NULL COMMENT '挂号序号',
    @ItemProperty(alias = "mpiId")
    private String mpiId;
    @ItemProperty(alias = "证件类型 1 身份证")
    private Integer certificateType; // tinyint(1) DEFAULT NULL COMMENT '证件类型 1 身份证',
    @ItemProperty(alias = "证件号")
    private String certificate; // varchar(18) DEFAULT NULL COMMENT '证件号',
    @ItemProperty(alias = "患者姓名")
    private String patientName; // varchar(32) NOT NULL COMMENT '患者姓名',
    @ItemProperty(alias = "患者手机号")
    private String patientTel; // varchar(20) DEFAULT NULL COMMENT '患者手机号',
    @ItemProperty(alias = "患者地址")
    private String patientAddress; // varchar(250) DEFAULT NULL COMMENT '患者地址',
    @ItemProperty(alias = "患者手机号")
    private String patientNumber; // varchar(20) DEFAULT NULL COMMENT '患者手机号',
    @ItemProperty(alias = "His处方单号")
    private String recipeCode; // varchar(50) DEFAULT NULL COMMENT 'His处方单号',
    @ItemProperty(alias = "处方金额")
    private BigDecimal recipeFee; // decimal(10,2) DEFAULT NULL COMMENT '处方金额',
    @ItemProperty(alias = "开方机构序号")
    private  Integer clinicOrgan; // int(11) DEFAULT NULL COMMENT '开方机构序号',
    @ItemProperty(alias = "机构名称")
    private String organName; // varchar(50) DEFAULT NULL COMMENT '机构名称',
    @ItemProperty(alias = "1西药  2中成药 3 草药")
    private Integer recipeType; // tinyint(1) DEFAULT NULL COMMENT '1西药  2中成药 3 草药',
    @ItemProperty(alias = "科室编码")
    private String departCode; // varchar(10) DEFAULT NULL COMMENT '科室编码',
    @ItemProperty(alias = "科室名称")
    private String departName; // varchar(20) DEFAULT NULL COMMENT '科室名称',
    @ItemProperty(alias = "开方医生工号")
    private String doctorCode; // varchar(20) DEFAULT NULL COMMENT '开方医生工号',
    @ItemProperty(alias = "开方医生姓名")
    private String doctorName; // varchar(50) DEFAULT NULL COMMENT '开方医生姓名',
    @ItemProperty(alias = "创建时间")
    private Date createDate; // datetime DEFAULT NULL,
    @ItemProperty(alias = "诊断编码")
    private String disease; // varchar(50) DEFAULT NULL COMMENT '诊断编码',
    @ItemProperty(alias = "诊断编码")
    private String diseaseName; // varchar(50) DEFAULT NULL COMMENT '诊断名称',
    @ItemProperty(alias = "诊断备注")
    private String memo; // varchar(255) DEFAULT NULL COMMENT '诊断备注',
    @ItemProperty(alias = "处方备注")
    private String recipeMemo; // varchar(250) DEFAULT NULL COMMENT '处方备注',
    @ItemProperty(alias = "1 未处理 2已处理")
    private Integer status; //tinyint(4) NOT NULL DEFAULT '0' COMMENT '1 未处理 2已处理',
    @ItemProperty(alias = "中药处方用法")
    private String tcmUsePathways; //varchar(30) DEFAULT NULL COMMENT '中药处方用法',
    @ItemProperty(alias = "中药处方用量")
    private String tcmUsingRate; // varchar(20) DEFAULT NULL COMMENT '中药处方用量',
    @ItemProperty(alias = "贴数")
    private String tcmNum; // int(11) DEFAULT '0' COMMENT '贴数',
    @ItemProperty(alias = "结算类型/医保类型 1 自费  2 医保")
    private Integer medicalType; // tinyint(4) NOT NULL DEFAULT '1' COMMENT '1 自费  2 医保',
    @ItemProperty(alias = "提示文本")
    private String showText; // varchar(250) DEFAULT NULL COMMENT '提示文本',
    @ItemProperty(alias = "是否外延处方")
    private Integer extensionFlag; // tinyint(4) NOT NULL DEFAULT '1' COMMENT '是否外延处方 0 否  1 是',
    @ItemProperty(alias = "处方流水号")
    private String tradeNo; // varchar(50) DEFAULT NULL COMMENT '处方流水号',
    @ItemProperty(alias = "医保报销金额")
    private BigDecimal medicalAmount; // decimal(10,2) DEFAULT NULL COMMENT '医保报销金额',
    @ItemProperty(alias = "自费金额")
    private BigDecimal cashAmount; // decimal(10,2) DEFAULT NULL COMMENT '自费金额',
    @ItemProperty(alias = "总金额")
    private BigDecimal totalAmount; // decimal(10,2) DEFAULT NULL COMMENT '总金额',
    @ItemProperty(alias = "订单状态描述")
    private String orderStatusText;
    @ItemProperty(alias = "处方来源")
    private Integer fromFlag;
    @ItemProperty(alias = "插入时间")
    private Date createTime;
    @ItemProperty(alias = "处方支付类型 0 普通支付 1 不选择购药方式直接去支付")
    private Integer recipePayType;
    @ItemProperty(alias = "配送方式/购药方式 1配送到家 2医院取药 3药店取药")
    private Integer giveMode;
    @ItemProperty(alias = "配送药企代码")
    private String deliveryCode;
    @ItemProperty(alias = "配送药企名称")
    private String deliveryName;
    @ItemProperty(alias = "配送地址")
    private String sendAddr;
    @ItemProperty(alias = "处方单特殊来源标识：1省中，邵逸夫医保小程序;  2北京 默认null")
    private Integer recipeSource;
    @ItemProperty(alias = "收货人姓名")
    private String receiverName;
    @ItemProperty(alias = "收货人电话")
    private String receiverTel;
    @ItemProperty(alias = "是否缓存在平台")
    private Integer isCachePlatform;

    //中药
    @ItemProperty(alias = "中医辨证论治费")
    private BigDecimal tcmFee;
    @ItemProperty(alias = "代煎费")
    private BigDecimal decoctionFee;
    @ItemProperty(alias = "his处方付费序号合集")
    private String recipeCostNumber;
    @ItemProperty(alias = "煎法编码")
    private String decoctionCode;
    @ItemProperty(alias = "煎法名称")
    private String decoctionText;
    @ItemProperty(alias = "药师姓名")
    private String checkerName;
    @ItemProperty(alias = "药师工号")
    private String checkerCode;
    @ItemProperty(alias = "病种代码")
    private String chronicDiseaseCode;
    @ItemProperty(alias = "病种名称")
    private String chronicDiseaseName;

    @ItemProperty(alias = "制法")
    private String makeMethodCode;
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
    private String symptomCode;
    @ItemProperty(alias = "中医症候名称")
    private String symptomName;
    @ItemProperty(alias = "特殊煎法code：搅碎、碾磨等")
    private String specialDecoctionCode;
    @ItemProperty(alias = "卡号")
    private String cardNo;
    @ItemProperty(alias = "卡类型")
    private String cardTypeCode;
    @ItemProperty(alias = "卡名称")
    private String cardTypeName;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "hisRecipeID", unique = true, nullable = false)
    public Integer getHisRecipeID() {
        return hisRecipeID;
    }

    public void setHisRecipeID(Integer hisRecipeID) {
        this.hisRecipeID = hisRecipeID;
    }

    @Column(name = "registeredId")
    public String getRegisteredId() {
        return registeredId;
    }

    public void setRegisteredId(String registeredId) {
        this.registeredId = registeredId;
    }

    @Column(name = "mpiId")
    public String getMpiId() {
        return mpiId;
    }

    public void setMpiId(String mpiId) {
        this.mpiId = mpiId;
    }

    @Column(name = "certificateType")
    public Integer getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(Integer certificateType) {
        this.certificateType = certificateType;
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

    @Column(name = "patientAddress")
    public String getPatientAddress() {
        return patientAddress;
    }

    public void setPatientAddress(String patientAddress) {
        this.patientAddress = patientAddress;
    }

    @Column(name = "patientNumber")
    public String getPatientNumber() {
        return patientNumber;
    }

    public void setPatientNumber(String patientNumber) {
        this.patientNumber = patientNumber;
    }

    @Column(name = "recipeCode")
    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    @Column(name = "recipeFee")
    public BigDecimal getRecipeFee() {
        return recipeFee;
    }

    public void setRecipeFee(BigDecimal recipeFee) {
        this.recipeFee = recipeFee;
    }

    @Column(name = "clinicOrgan")
    public Integer getClinicOrgan() {
        return clinicOrgan;
    }

    public void setClinicOrgan(Integer clinicOrgan) {
        this.clinicOrgan = clinicOrgan;
    }

    @Column(name = "organName")
    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    @Column(name = "recipeType")
    public Integer getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(Integer recipeType) {
        this.recipeType = recipeType;
    }

    @Column(name = "departCode")
    public String getDepartCode() {
        return departCode;
    }

    public void setDepartCode(String departCode) {
        this.departCode = departCode;
    }

    @Column(name = "departName")
    public String getDepartName() {
        return departName;
    }

    public void setDepartName(String departName) {
        this.departName = departName;
    }

    @Column(name = "doctorCode")
    public String getDoctorCode() {
        return doctorCode;
    }

    public void setDoctorCode(String doctorCode) {
        this.doctorCode = doctorCode;
    }

    @Column(name = "doctorName")
    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    @Column(name = "createDate")
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Column(name = "disease")
    public String getDisease() {
        return disease;
    }

    public void setDisease(String disease) {
        this.disease = disease;
    }

    @Column(name = "diseaseName")
    public String getDiseaseName() {
        return diseaseName;
    }

    public void setDiseaseName(String diseaseName) {
        this.diseaseName = diseaseName;
    }

    @Column(name = "memo")
    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    @Column(name = "recipeMemo")
    public String getRecipeMemo() {
        return recipeMemo;
    }

    public void setRecipeMemo(String recipeMemo) {
        this.recipeMemo = recipeMemo;
    }

    @Column(name = "status")
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Column(name = "tcmUsePathways")
    public String getTcmUsePathways() {
        return tcmUsePathways;
    }

    public void setTcmUsePathways(String tcmUsePathways) {
        this.tcmUsePathways = tcmUsePathways;
    }

    @Column(name = "tcmUsingRate")
    public String getTcmUsingRate() {
        return tcmUsingRate;
    }

    public void setTcmUsingRate(String tcmUsingRate) {
        this.tcmUsingRate = tcmUsingRate;
    }

    @Column(name = "tcmNum")
    public String getTcmNum() {
        return tcmNum;
    }

    public void setTcmNum(String tcmNum) {
        this.tcmNum = tcmNum;
    }

    @Column(name = "medicalType")
    public Integer getMedicalType() {
        return medicalType;
    }

    public void setMedicalType(Integer medicalType) {
        this.medicalType = medicalType;
    }

    @Column(name = "showText")
    public String getShowText() {
        return showText;
    }

    public void setShowText(String showText) {
        this.showText = showText;
    }

    @Column(name = "extensionFlag")
    public Integer getExtensionFlag() {
        return extensionFlag;
    }

    public void setExtensionFlag(Integer extensionFlag) {
        this.extensionFlag = extensionFlag;
    }

    @Column(name = "tradeNo")
    public String getTradeNo() {
        return tradeNo;
    }

    public void setTradeNo(String tradeNo) {
        this.tradeNo = tradeNo;
    }

    @Column(name = "medicalAmount")
    public BigDecimal getMedicalAmount() {
        return medicalAmount;
    }

    public void setMedicalAmount(BigDecimal medicalAmount) {
        this.medicalAmount = medicalAmount;
    }

    @Column(name = "cashAmount")
    public BigDecimal getCashAmount() {
        return cashAmount;
    }

    public void setCashAmount(BigDecimal cashAmount) {
        this.cashAmount = cashAmount;
    }

    @Column(name = "totalAmount")
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    @Column(name = "createTime")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Column(name = "recipePayType")
    public Integer getRecipePayType() {
        return recipePayType;
    }

    public void setRecipePayType(Integer recipePayType) {
        this.recipePayType = recipePayType;
    }

    @Transient
    public String getOrderStatusText() {
        return orderStatusText;
    }

    public void setOrderStatusText(String orderStatusText) {
        this.orderStatusText = orderStatusText;
    }

    @Transient
    public Integer getFromFlag() {
        return fromFlag;
    }

    public void setFromFlag(Integer fromFlag) {
        this.fromFlag = fromFlag;
    }

    public Integer getGiveMode() {
        return giveMode;
    }

    public void setGiveMode(Integer giveMode) {
        this.giveMode = giveMode;
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

    public String getSendAddr() {
        return sendAddr;
    }

    public void setSendAddr(String sendAddr) {
        this.sendAddr = sendAddr;
    }

    public Integer getRecipeSource() {
        return recipeSource;
    }

    public void setRecipeSource(Integer recipeSource) {
        this.recipeSource = recipeSource;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverTel() {
        return receiverTel;
    }

    public void setReceiverTel(String receiverTel) {
        this.receiverTel = receiverTel;
    }

    @Transient
    public Integer getIsCachePlatform() {
        return isCachePlatform;
    }

    public void setIsCachePlatform(Integer cachePlatform) {
        isCachePlatform = cachePlatform;
    }

    public BigDecimal getTcmFee() {
        return tcmFee;
    }

    public void setTcmFee(BigDecimal tcmFee) {
        this.tcmFee = tcmFee;
    }

    public BigDecimal getDecoctionFee() {
        return decoctionFee;
    }

    public void setDecoctionFee(BigDecimal decoctionFee) {
        this.decoctionFee = decoctionFee;
    }

    public String getRecipeCostNumber() {
        return recipeCostNumber;
    }

    public void setRecipeCostNumber(String recipeCostNumber) {
        this.recipeCostNumber = recipeCostNumber;
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

    @Column(name = "checkerName")
    public String getCheckerName() {
        return checkerName;
    }

    public void setCheckerName(String checkerName) {
        this.checkerName = checkerName;
    }

    @Column(name = "checkerCode")
    public String getCheckerCode() {
        return checkerCode;
    }

    public void setCheckerCode(String checkerCode) {
        this.checkerCode = checkerCode;
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

    @Column(name = "makeMethodCode")
    public String getMakeMethodCode() {
        return makeMethodCode;
    }

    public void setMakeMethodCode(String makeMethodCode) {
        this.makeMethodCode = makeMethodCode;
    }

    @Column(name = "makeMethodText")
    public String getMakeMethodText() {
        return makeMethodText;
    }

    public void setMakeMethodText(String makeMethodText) {
        this.makeMethodText = makeMethodText;
    }

    @Column(name = "juice")
    public String getJuice() {
        return juice;
    }

    public void setJuice(String juice) {
        this.juice = juice;
    }

    @Column(name = "juiceUnit")
    public String getJuiceUnit() {
        return juiceUnit;
    }

    public void setJuiceUnit(String juiceUnit) {
        this.juiceUnit = juiceUnit;
    }

    @Column(name = "minor")
    public String getMinor() {
        return minor;
    }

    public void setMinor(String minor) {
        this.minor = minor;
    }

    @Column(name = "minorUnit")
    public String getMinorUnit() {
        return minorUnit;
    }

    public void setMinorUnit(String minorUnit) {
        this.minorUnit = minorUnit;
    }

    @Column(name = "symptomCode")
    public String getSymptomCode() {
        return symptomCode;
    }

    public void setSymptomCode(String symptomCode) {
        this.symptomCode = symptomCode;
    }
    @Column(name = "symptomName")
    public String getSymptomName() {
        return symptomName;
    }

    public void setSymptomName(String symptomName) {
        this.symptomName = symptomName;
    }

    @Column(name = "specialDecoctionCode")
    public String getSpecialDecoctionCode() {
        return specialDecoctionCode;
    }

    public void setSpecialDecoctionCode(String specialDecoctionCode) {
        this.specialDecoctionCode = specialDecoctionCode;
    }

    @Column(name = "cardNo")
    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    @Column(name = "cardTypeCode")
    public String getCardTypeCode() {
        return cardTypeCode;
    }

    public void setCardTypeCode(String cardTypeCode) {
        this.cardTypeCode = cardTypeCode;
    }

    @Column(name = "cardTypeName")
    public String getCardTypeName() {
        return cardTypeName;
    }

    public void setCardTypeName(String cardTypeName) {
        this.cardTypeName = cardTypeName;
    }
}
