package com.ngari.recipe.entity;

import ctd.account.session.ClientSession;
import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.FileToken;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author yuyun
 */
@Entity
@Schema
@Table(name = "cdr_recipe")
@Access(AccessType.PROPERTY)
@NoArgsConstructor
public class Recipe implements Serializable {

    private static final long serialVersionUID = -6170665419368031590L;

    @ItemProperty(alias = "处方序号")
    private Integer recipeId;

    @ItemProperty(alias = "订单编号")
    private String orderCode;

    @ItemProperty(alias = "开处方来源 1问诊 2复诊(在线续方) 3网络门诊 5门诊 ")
    @Dictionary(id = "eh.cdr.dictionary.BussSourceType")
    private Integer bussSource;

    @ItemProperty(alias = "就诊序号(对应来源的业务id)")
    private Integer clinicId;

    @ItemProperty(alias = "主索引（患者编号）")
    private String mpiid;

    @ItemProperty(alias = "患者医院病历号")
    private String patientID;

    @ItemProperty(alias = "患者状态 1正常  9注销")
    private Integer patientStatus;

    @ItemProperty(alias = "开方机构")
    @Dictionary(id = "eh.base.dictionary.Organ")
    private Integer clinicOrgan;

    @ItemProperty(alias = "开方机构名称")
    private String organName;

    @ItemProperty(alias = "处方来源机构")
    @Dictionary(id = "eh.base.dictionary.Organ")
    private Integer originClinicOrgan;

    @ItemProperty(alias = "处方号码，处方回写")
    private String recipeCode;

    @ItemProperty(alias = "处方来源源处方号")
    private String originRecipeCode;

    @ItemProperty(alias = "处方类型 1 西药 2 中成药")
    @Dictionary(id = "eh.cdr.dictionary.RecipeType")
    private Integer recipeType;

    @ItemProperty(alias = "处方流转模式")
    private String recipeMode;

    @ItemProperty(alias = "开方科室")
    @Dictionary(id = "eh.base.dictionary.Depart")
    private Integer depart;

    @ItemProperty(alias = "挂号科室")
    private String appointDepart;

    @ItemProperty(alias = "挂号科室名称")
    private String appointDepartName;

    @ItemProperty(alias = "开方医生（医生Id）")
    @Dictionary(id = "eh.base.dictionary.Doctor")
    private Integer doctor;

    @ItemProperty(alias = "开方时间")
    private Date createDate;

    @ItemProperty(alias = "剂数")
    private Integer copyNum;

    @ItemProperty(alias = "处方金额")
    private BigDecimal totalMoney;

    @ItemProperty(alias = "机构疾病名称")
    private String organDiseaseName;

    @ItemProperty(alias = "机构疾病编码")
    private String organDiseaseId;

    @ItemProperty(alias = "支付标志")
    private Integer payFlag;

    @ItemProperty(alias = "支付日期")
    private Date payDate;

    @ItemProperty(alias = "发药机构")
    private Integer giveOrgan;

    @ItemProperty(alias = "发药标志")
    private Integer giveFlag;

    @ItemProperty(alias = "发药完成日期")
    private Date giveDate;

    @ItemProperty(alias = "有效天数")
    private Integer valueDays;

    @ItemProperty(alias = "审核机构")
    @Dictionary(id = "eh.base.dictionary.Organ")
    private Integer checkOrgan;

    @ItemProperty(alias = "审核日期")
    private Date checkDate;

    @ItemProperty(alias = "审核人")
    private Integer checker;

    @ItemProperty(alias = "审核药师姓名")
    private String checkerText;

    @ItemProperty(alias = "人工审核日期")
    private Date checkDateYs;

    @ItemProperty(alias = "药师电话")
    private String checkerTel;

    @ItemProperty(alias = "发药方式")
    private Integer giveMode;

    @ItemProperty(alias = "发药人姓名")
    private String giveUser;

    @ItemProperty(alias = "签名的处方PDF")
    @FileToken(expires = 3600)
    private String signFile;

    @ItemProperty(alias = "药师签名的处方PDF")
    @FileToken(expires = 3600)
    private String chemistSignFile;

    @ItemProperty(alias = "处方状态")
    @Dictionary(id = "eh.cdr.dictionary.RecipeStatus")
    private Integer status;

    @ItemProperty(alias = "来源标志")
    @Dictionary(id = "eh.cdr.dictionary.FromFlag")
    private Integer fromflag;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    @ItemProperty(alias = "开始配送时间")
    private Date startSendDate;

    @ItemProperty(alias = "配送人")
    private String sender;

    @ItemProperty(alias = "签名时间")
    private Date signDate;

    @ItemProperty(alias = "处方药品名称集合")
    private String recipeDrugName;

    @ItemProperty(alias = "前台页面展示的时间")
    private Date recipeShowTime;

    @ItemProperty(alias = "微信端展示过期时间，处方离过期剩余小时数")
    private String recipeSurplusHours;

    @ItemProperty(alias = "审核失败备注")
    private String checkFailMemo;

    @ItemProperty(alias = "药师审核不通过，医生补充说明")
    private String supplementaryMemo;

    @ItemProperty(alias = "用户选择购药标志位")
    private Integer chooseFlag;

    @ItemProperty(alias = "处方失效前提醒标志位")
    private Integer remindFlag;

    @ItemProperty(alias = "药企序号")
    private Integer enterpriseId;

    @ItemProperty(alias = "药企推送标志位, 0未推送，1已推送")
    private Integer pushFlag;

    @ItemProperty(alias = "药师审核不通过的旧处方Id")
    private Integer oldRecipeId;

    @ItemProperty(alias = "最后需支付费用")
    private BigDecimal actualPrice;

    @ItemProperty(alias = "订单总价")
    private BigDecimal orderAmount;

    @ItemProperty(alias = "优惠价格")
    private String discountAmount;

    @ItemProperty(alias = "诊断备注")
    private String memo;

    @ItemProperty(alias = "医保支付标志，1：可以用医保")
    private Integer medicalPayFlag;

    @ItemProperty(alias = "配送处方标记 默认0，1: 只能配送")
    private Integer distributionFlag;

    @ItemProperty(alias = "处方备注")
    private String recipeMemo;

    @ItemProperty(alias = "中药属性：用法")
    private String tcmUsePathways;

    @ItemProperty(alias = "中药属性：用量")
    private String tcmUsingRate;

    @ItemProperty(alias = "处方单状态显示")
    private String showTip;

    @ItemProperty(alias = "药店价格最低价")
    private BigDecimal price1;

    @ItemProperty(alias = "药店价格最高价")
    private BigDecimal price2;

    @ItemProperty(alias = "医生姓名")
    private String doctorName;

    @ItemProperty(alias = "患者姓名")
    private String patientName;

    @ItemProperty(alias = "外带处方标志 1:外带药处方")
    private Integer takeMedicine;

    @ItemProperty(alias = "处方发起者id,用户标识")
    private String requestMpiId;

    @ItemProperty(alias = "处方发起者urt")
    private Integer requestUrt;

    @ItemProperty(alias = "当前clientId")
    private Integer currentClient;

    @ItemProperty(alias = "监管平台同步标记: 0未同步，1已同步")
    private Integer syncFlag;

    @ItemProperty(alias = "签名的处方img")
    @FileToken(expires = 3600)
    private String signImg;
    /**
     * 添加属性 2019/08/29
     * 审方模式
     */
    @ItemProperty(alias = "审核模式")
    private Integer reviewType;

    @ItemProperty(alias = "审核途径 1平台审核 2his审核")
    private Integer checkMode;

    @ItemProperty(alias = "处方来源类型 1 平台处方 2 线下转线上的处方")
    private Integer recipeSourceType;


    /**
     * 添加属性 2019/10/10
     * 审核状态标记位，暂时标记一次审核不通过
     * 处方进行二次审核，一次审核不通过更新字段，二次审核通过/不通过再更新字段
     */
    @ItemProperty(alias = "审核状态标记位")
    private Integer checkStatus;

    @ItemProperty(alias = "处方单特殊来源标识：1省中，邵逸夫医保小程序;  2北京 默认null")
    private Integer recipeSource;

    @ItemProperty(alias = "医生处方数字签名值")
    private String signRecipeCode;

    @ItemProperty(alias = "药师处方数字签名值")
    private String signPharmacistCode;

    @ItemProperty(alias = "医生处方数字签名可信服务器时间")
    private String signCADate;

    @ItemProperty(alias = "药师处方数字签名可信服务器时间")
    private String signPharmacistCADate;

    @ItemProperty(alias = "处方支付类型 0 普通支付 1 不选择购药方式直接去支付")
    private Integer recipePayType;

    @ItemProperty(alias = "失效时间")
    private Date invalidTime;

    @ItemProperty(alias = "是否被接方 0 未接方 1已接方")
    private Integer grabOrderStatus;

    @ItemProperty(alias = "处方支持的购药方式,逗号分隔")
    private String recipeSupportGiveMode;

    @ItemProperty(alias = "处方审核状态")
    private Integer checkFlag;

    @Column(name = "recipeSupportGiveMode")
    public String getRecipeSupportGiveMode() {
        return recipeSupportGiveMode;
    }

    public void setRecipeSupportGiveMode(String recipeSupportGiveMode) {
        this.recipeSupportGiveMode = recipeSupportGiveMode;
    }

    @Column(name = "invalidTime")
    public Date getInvalidTime() {
        return invalidTime;
    }

    public void setInvalidTime(Date invalidTime) {
        this.invalidTime = invalidTime;
    }

    @Column(name = "checkStatus")
    public Integer getCheckStatus() {
        return checkStatus;
    }

    public void setCheckStatus(Integer checkStatus) {
        this.checkStatus = checkStatus;
    }

    @Column(name = "patientStatus")
    public Integer getPatientStatus() {
        return patientStatus;
    }

    public void setPatientStatus(Integer patientStatus) {
        this.patientStatus = patientStatus;
    }

    @Transient
    public BigDecimal getPrice1() {
        return price1;
    }

    public void setPrice1(BigDecimal price1) {
        this.price1 = price1;
    }

    @Transient
    public BigDecimal getPrice2() {
        return price2;
    }

    public void setPrice2(BigDecimal price2) {
        this.price2 = price2;
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "RecipeID", unique = true, nullable = false)
    public Integer getRecipeId() {
        return this.recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    @Column(name = "OrderCode")
    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    @Column(name = "ClinicID")
    public Integer getClinicId() {
        return this.clinicId;
    }

    public void setClinicId(Integer clinicId) {
        this.clinicId = clinicId;
    }

    @Column(name = "MPIID", nullable = false)
    public String getMpiid() {
        return this.mpiid;
    }

    public void setMpiid(String mpiid) {
        this.mpiid = mpiid;
    }

    @Column(name = "PatientID")
    public String getPatientID() {
        return patientID;
    }

    public void setPatientID(String patientID) {
        this.patientID = patientID;
    }

    @Column(name = "ClinicOrgan", nullable = false)
    public Integer getClinicOrgan() {
        return this.clinicOrgan;
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

    @Column(name = "OriginClinicOrgan")
    public Integer getOriginClinicOrgan() {
        return originClinicOrgan;
    }

    public void setOriginClinicOrgan(Integer originClinicOrgan) {
        this.originClinicOrgan = originClinicOrgan;
    }

    @Column(name = "RecipeCode")
    public String getRecipeCode() {
        return this.recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    @Column(name = "OriginRecipeCode")
    public String getOriginRecipeCode() {
        return originRecipeCode;
    }

    public void setOriginRecipeCode(String originRecipeCode) {
        this.originRecipeCode = originRecipeCode;
    }

    @Column(name = "RecipeType", length = 10)
    public Integer getRecipeType() {
        return this.recipeType;
    }

    public void setRecipeType(Integer recipeType) {
        this.recipeType = recipeType;
    }

    @Column(name = "recipeMode")
    public String getRecipeMode() {
        return recipeMode;
    }

    public void setRecipeMode(String recipeMode) {
        this.recipeMode = recipeMode;
    }

    @Column(name = "Depart")
    public Integer getDepart() {
        return this.depart;
    }

    public void setDepart(Integer depart) {
        this.depart = depart;
    }

    @Column(name = "appoint_depart")
    public String getAppointDepart() {
        return appointDepart;
    }

    public void setAppointDepart(String appointDepart) {
        this.appointDepart = appointDepart;
    }

    @Column(name = "appoint_depart_name")
    public String getAppointDepartName() {
        return appointDepartName;
    }

    public void setAppointDepartName(String appointDepartName) {
        this.appointDepartName = appointDepartName;
    }

    @Column(name = "Doctor")
    public Integer getDoctor() {
        return this.doctor;
    }

    public void setDoctor(Integer doctor) {
        this.doctor = doctor;
    }

    @Column(name = "CreateDate")
    public Date getCreateDate() {
        return this.createDate;
    }

    public void setCreateDate(Date date) {
        this.createDate = date;
    }

    @Column(name = "CopyNum")
    public Integer getCopyNum() {
        return this.copyNum;
    }

    public void setCopyNum(Integer copyNum) {
        this.copyNum = copyNum;
    }

    @Column(name = "TotalMoney", precision = 10)
    public BigDecimal getTotalMoney() {
        return totalMoney;
    }

    public void setTotalMoney(BigDecimal totalMoney) {
        this.totalMoney = totalMoney;
    }

    @Column(name = "OrganDiseaseName")
    public String getOrganDiseaseName() {
        return this.organDiseaseName;
    }

    public void setOrganDiseaseName(String organDiseaseName) {
        this.organDiseaseName = organDiseaseName;
    }

    @Column(name = "OrganDiseaseID")
    public String getOrganDiseaseId() {
        return this.organDiseaseId;
    }

    public void setOrganDiseaseId(String organDiseaseId) {
        this.organDiseaseId = organDiseaseId;
    }

    @Column(name = "PayFlag")
    public Integer getPayFlag() {
        return this.payFlag;
    }

    public void setPayFlag(Integer payFlag) {
        this.payFlag = payFlag;
    }

    @Column(name = "PayDate")
    public Date getPayDate() {
        return this.payDate;
    }

    public void setPayDate(Date payDate) {
        this.payDate = payDate;
    }

    @Column(name = "GiveOrgan")
    public Integer getGiveOrgan() {
        return this.giveOrgan;
    }

    public void setGiveOrgan(Integer giveOrgan) {
        this.giveOrgan = giveOrgan;
    }

    @Column(name = "GiveFlag")
    public Integer getGiveFlag() {
        return this.giveFlag;
    }

    public void setGiveFlag(Integer giveFlag) {
        this.giveFlag = giveFlag;
    }

    @Column(name = "CheckOrgan")
    public Integer getCheckOrgan() {
        return checkOrgan;
    }

    public void setCheckOrgan(Integer checkOrgan) {
        this.checkOrgan = checkOrgan;
    }

    @Column(name = "CheckDate")
    public Date getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(Date checkDate) {
        this.checkDate = checkDate;
    }

    @Column(name = "Checker")
    public Integer getChecker() {
        return checker;
    }

    public void setChecker(Integer checker) {
        this.checker = checker;
    }

    @Column(name = "CheckerText")
    public String getCheckerText() {
        return checkerText;
    }

    public void setCheckerText(String checkerText) {
        this.checkerText = checkerText;
    }

    @Column(name = "CheckDateYs")
    public Date getCheckDateYs() {
        return checkDateYs;
    }

    public void setCheckDateYs(Date checkDateYs) {
        this.checkDateYs = checkDateYs;
    }

    @Transient
    public String getCheckerTel() {
        return checkerTel;
    }

    public void setCheckerTel(String checkerTel) {
        this.checkerTel = checkerTel;
    }

    @Column(name = "GiveMode")
    public Integer getGiveMode() {
        return giveMode;
    }

    public void setGiveMode(Integer giveMode) {
        this.giveMode = giveMode;
    }

    @Column(name = "GiveUser", length = 20)
    public String getGiveUser() {
        return giveUser;
    }

    public void setGiveUser(String giveUser) {
        this.giveUser = giveUser;
    }

    @Column(name = "SignFile")
    public String getSignFile() {
        return signFile;
    }

    public void setSignFile(String signFile) {
        this.signFile = signFile;
    }

    @Column(name = "ChemistSignFile")
    public String getChemistSignFile() {
        return chemistSignFile;
    }

    public void setChemistSignFile(String chemistSignFile) {
        this.chemistSignFile = chemistSignFile;
    }

    @Column(name = "Status")
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Column(name = "GiveDate")
    public Date getGiveDate() {
        return this.giveDate;
    }

    public void setGiveDate(Date giveDate) {
        this.giveDate = giveDate;
    }

    @Column(name = "ValueDays")
    public Integer getValueDays() {
        return this.valueDays;
    }

    public void setValueDays(Integer valueDays) {
        this.valueDays = valueDays;
    }

    @Column(name = "fromflag")
    public Integer getFromflag() {
        return fromflag;
    }

    public void setFromflag(Integer fromflag) {
        this.fromflag = fromflag;
    }

    @Column(name = "LastModify")
    public Date getLastModify() {
        return this.lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    @Column(name = "startSendDate")
    public Date getStartSendDate() {
        return this.startSendDate;
    }

    public void setStartSendDate(Date startSendDate) {
        this.startSendDate = startSendDate;
    }

    @Column(name = "SignDate")
    public Date getSignDate() {
        return signDate;
    }

    public void setSignDate(Date signDate) {
        this.signDate = signDate;
    }

    @Transient
    public String getRecipeDrugName() {
        return recipeDrugName;
    }

    public void setRecipeDrugName(String recipeDrugName) {
        this.recipeDrugName = recipeDrugName;
    }

    @Transient
    public Date getRecipeShowTime() {
        return recipeShowTime;
    }

    public void setRecipeShowTime(Date recipeShowTime) {
        this.recipeShowTime = recipeShowTime;
    }

    @Transient
    public String getRecipeSurplusHours() {
        return recipeSurplusHours;
    }

    public void setRecipeSurplusHours(String recipeSurplusHours) {
        this.recipeSurplusHours = recipeSurplusHours;
    }

    @Column(name = "CheckFailMemo")
    public String getCheckFailMemo() {
        return checkFailMemo;
    }

    public void setCheckFailMemo(String checkFailMemo) {
        this.checkFailMemo = checkFailMemo;
    }

    @Column(name = "SupplementaryMemo")
    public String getSupplementaryMemo() {
        return supplementaryMemo;
    }

    public void setSupplementaryMemo(String supplementaryMemo) {
        this.supplementaryMemo = supplementaryMemo;
    }

    @Column(name = "ChooseFlag")
    public Integer getChooseFlag() {
        return chooseFlag;
    }

    public void setChooseFlag(Integer chooseFlag) {
        this.chooseFlag = chooseFlag;
    }

    @Column(name = "RemindFlag")
    public Integer getRemindFlag() {
        return remindFlag;
    }

    public void setRemindFlag(Integer remindFlag) {
        this.remindFlag = remindFlag;
    }

    @Column(name = "Sender")
    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    @Column(name = "EnterpriseId")
    public Integer getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(Integer enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    @Column(name = "PushFlag")
    public Integer getPushFlag() {
        return pushFlag;
    }

    public void setPushFlag(Integer pushFlag) {
        this.pushFlag = pushFlag;
    }

    @Column(name = "OldRecipeId")
    public Integer getOldRecipeId() {
        return oldRecipeId;
    }

    public void setOldRecipeId(Integer oldRecipeId) {
        this.oldRecipeId = oldRecipeId;
    }

    @Column(name = "ActualPrice")
    public BigDecimal getActualPrice() {
        return actualPrice;
    }

    public void setActualPrice(BigDecimal actualPrice) {
        this.actualPrice = actualPrice;
    }

    @Transient
    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }

    @Transient
    public String getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(String discountAmount) {
        this.discountAmount = discountAmount;
    }

    @Transient
    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    @Column(name = "MedicalPayFlag")
    public Integer getMedicalPayFlag() {
        return medicalPayFlag;
    }

    public void setMedicalPayFlag(Integer medicalPayFlag) {
        this.medicalPayFlag = medicalPayFlag;
    }

    public boolean canMedicalPay() {
        Integer useMedicalFlag = 1;
        return (useMedicalFlag.equals(medicalPayFlag)) ? true : false;
    }

    @Column(name = "DistributionFlag")
    public Integer getDistributionFlag() {
        return distributionFlag;
    }

    public void setDistributionFlag(Integer distributionFlag) {
        this.distributionFlag = distributionFlag;
    }

    @Column(name = "RecipeMemo")
    public String getRecipeMemo() {
        return recipeMemo;
    }

    public void setRecipeMemo(String recipeMemo) {
        this.recipeMemo = recipeMemo;
    }

    @Transient
    public String getTcmUsePathways() {
        return tcmUsePathways;
    }

    public void setTcmUsePathways(String tcmUsePathways) {
        this.tcmUsePathways = tcmUsePathways;
    }

    @Transient
    public String getTcmUsingRate() {
        return tcmUsingRate;
    }

    public void setTcmUsingRate(String tcmUsingRate) {
        this.tcmUsingRate = tcmUsingRate;
    }

    @Transient
    public String getShowTip() {
        return showTip;
    }

    public void setShowTip(String showTip) {
        this.showTip = showTip;
    }

    @Column(name = "doctorName")
    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    @Column(name = "patientName")
    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    @Column(name = "TakeMedicine")
    public Integer getTakeMedicine() {
        return takeMedicine;
    }

    public void setTakeMedicine(Integer takeMedicine) {
        this.takeMedicine = takeMedicine;
    }

    @Column(name = "requestMpiId")
    public String getRequestMpiId() {
        return requestMpiId;
    }

    public void setRequestMpiId(String requestMpiId) {
        this.requestMpiId = requestMpiId;
    }

    @Column(name = "requestUrt")
    public Integer getRequestUrt() {
        return requestUrt;
    }

    public void setRequestUrt(Integer requestUrt) {
        this.requestUrt = requestUrt;
    }

    @Column(name = "currentClient")
    public Integer getCurrentClient() {
        return currentClient == null ? ClientSession.getCurrentId() : currentClient;
    }

    public void setCurrentClient(Integer currentClient) {
        this.currentClient = currentClient;
    }

    @Column(name = "syncFlag")
    public Integer getSyncFlag() {
        return syncFlag;
    }

    public void setSyncFlag(Integer syncFlag) {
        this.syncFlag = syncFlag;
    }

    @Column(name = "signImg")
    public String getSignImg() {
        return signImg;
    }

    public void setSignImg(String signImg) {
        this.signImg = signImg;
    }

    @Column(name = "reviewType")
    public Integer getReviewType() {
        return reviewType;
    }

    public void setReviewType(Integer reviewType) {
        this.reviewType = reviewType;
    }

    @Column(name = "bussSource")
    public Integer getBussSource() {
        return bussSource;
    }

    public void setBussSource(Integer bussSource) {
        this.bussSource = bussSource;
    }

    @Column(name = "recipeSource")
    public Integer getRecipeSource() {
        return recipeSource;
    }

    public void setRecipeSource(Integer recipeSource) {
        this.recipeSource = recipeSource;
    }

    @Column(name = "checkMode")
    public Integer getCheckMode() {
        return checkMode;
    }

    public void setCheckMode(Integer checkMode) {
        this.checkMode = checkMode;
    }

    @Column(name = "signRecipeCode")
    public String getSignRecipeCode() {
        return signRecipeCode;
    }

    public void setSignRecipeCode(String signRecipeCode) {
        this.signRecipeCode = signRecipeCode;
    }

    @Column(name = "signPharmacistCode")
    public String getSignPharmacistCode() {
        return signPharmacistCode;
    }

    public void setSignPharmacistCode(String signPharmacistCode) {
        this.signPharmacistCode = signPharmacistCode;
    }

    @Column(name = "signCADate")
    public String getSignCADate() {
        return signCADate;
    }

    public void setSignCADate(String signCADate) {
        this.signCADate = signCADate;
    }

    @Column(name = "signPharmacistCADate")
    public String getSignPharmacistCADate() {
        return signPharmacistCADate;
    }

    public void setSignPharmacistCADate(String signPharmacistCADate) {
        this.signPharmacistCADate = signPharmacistCADate;
    }

    @Column(name = "recipeSourceType")
    public Integer getRecipeSourceType() {
        return recipeSourceType;
    }

    public void setRecipeSourceType(Integer recipeSourceType) {
        this.recipeSourceType = recipeSourceType;
    }

    @Column(name = "recipePayType")
    public Integer getRecipePayType() {
        return recipePayType;
    }

    public void setRecipePayType(Integer recipePayType) {
        this.recipePayType = recipePayType;
    }

    @Column(name = "grabOrderStatus")
    public Integer getGrabOrderStatus() {
        return grabOrderStatus;
    }
    public void setGrabOrderStatus(Integer grabOrderStatus) {
        this.grabOrderStatus = grabOrderStatus;
    }

    @Column(name = "checkFlag")
    public Integer getCheckFlag() {
        return checkFlag;
    }

    public void setCheckFlag(Integer checkFlag) {
        this.checkFlag = checkFlag;
    }

    public Recipe(Integer recipeId, String supplementaryMemo) {
        this.recipeId = recipeId;
        this.supplementaryMemo = supplementaryMemo;
    }
    public Recipe(Integer recipeId, Integer clinicOrgan, Integer recipeType) {
        this.recipeId = recipeId;
        this.clinicOrgan = clinicOrgan;
        this.recipeType = recipeType;
    }
    public Recipe(Integer recipeId, Date signDate) {
        this.recipeId = recipeId;
        this.signDate = signDate;
    }
}

