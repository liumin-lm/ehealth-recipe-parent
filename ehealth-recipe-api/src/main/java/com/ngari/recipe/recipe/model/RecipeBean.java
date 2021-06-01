package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.FileToken;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author yuyun
 */
@Schema
public class RecipeBean implements Serializable {

    private static final long serialVersionUID = -8882418262625511818L;

    @ItemProperty(alias = "处方序号")
    private Integer recipeId;

    @ItemProperty(alias = "处方序号(加密后)")
    private String recipeIdE;

    @ItemProperty(alias = "订单编号")
    private String orderCode;

    @ItemProperty(alias = "开处方来源 1问诊 2复诊(在线续方) 3网络门诊")
    private Integer bussSource;

    @ItemProperty(alias = "就诊序号(对应来源的业务id)")
    private Integer clinicId;

    @ItemProperty(alias = "主索引")
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

    @ItemProperty(alias = "处方号码")
    private String recipeCode;

    @ItemProperty(alias = "处方来源源处方号")
    private String originRecipeCode;

    @ItemProperty(alias = "处方类型")
    @Dictionary(id = "eh.cdr.dictionary.RecipeType")
    private Integer recipeType;

    @ItemProperty(alias = "处方流转模式")
    private String recipeMode;

    @ItemProperty(alias = "开方科室")
    @Dictionary(id = "eh.base.dictionary.Depart")
    private Integer depart;

    @ItemProperty(alias = "开方医生")
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

    @ItemProperty(alias = "结算单号")
    private Integer payListId;

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

    @ItemProperty(alias = "支付方式")
    @Dictionary(id = "eh.cdr.dictionary.PayMode")
    private Integer payMode;

    @ItemProperty(alias = "发药方式")
    private Integer giveMode;

    @ItemProperty(alias = "发药方式文案")
    private String giveModeText;

    @ItemProperty(alias = "发药人姓名")
    private String giveUser;

    @ItemProperty(alias = "签名的处方PDF")
    @FileToken(expires = 3600)
    private String signFile;

    @ItemProperty(alias = "药师签名的处方PDF")
    @FileToken(expires = 3600)
    private String chemistSignFile;

    @ItemProperty(alias = "收货人")
    private String receiver;

    @ItemProperty(alias = "收货人手机号")
    private String recMobile;

    @ItemProperty(alias = "收货人电话")
    private String recTel;

    @ItemProperty(alias = "地址（省）")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address1;

    @ItemProperty(alias = "地址（市）")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address2;

    @ItemProperty(alias = "地址（区县）")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address3;

    @ItemProperty(alias = "详细地址")
    private String address4;

    @ItemProperty(alias = "邮政编码")
    private String zipCode;

    @ItemProperty(alias = "地址信息ID")
    private Integer addressId;

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

    @ItemProperty(alias = "开始发药时间")
    private Date sendDate;

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

    @ItemProperty(alias = "交易流水号")
    private String tradeNo;

    @ItemProperty(alias = "微信支付方式")
    private String wxPayWay;

    @ItemProperty(alias = "商户订单号")
    private String outTradeNo;

    @ItemProperty(alias = "支付机构id")
    private String payOrganId;

    @ItemProperty(alias = "微信支付错误码")
    private String wxPayErrorCode;

    @ItemProperty(alias = "药企序号")
    private Integer enterpriseId;

    @ItemProperty(alias = "药企推送标志位, 0未推送，1已推送")
    private Integer pushFlag;

    @ItemProperty(alias = "药师审核不通过的旧处方Id")
    private Integer oldRecipeId;

    @ItemProperty(alias = "优惠券Id")
    private Integer couponId;

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

    @ItemProperty(alias = "配送处方标记 默认0，1: 只能配送,2:只能到院取药")
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

    @ItemProperty(alias = "处方发起者id")
    private String requestMpiId;

    @ItemProperty(alias = "处方发起者urt")
    private Integer requestUrt;

    @ItemProperty(alias="当前clientId")
    private Integer currentClient;

    @ItemProperty(alias="前端页面跳转标记")
    private int notation;

    @ItemProperty(alias="处方详情信息")
    private RecipeExtendBean recipeExtend;

    @ItemProperty(alias="处方审核方式")
    private Integer reviewType;

    @ItemProperty(alias = "审核途径 1平台审核 2his审核")
    private Integer checkMode;

    @ItemProperty(alias="处方审核状态")
    private Integer checkStatus;

    @ItemProperty(alias = "处方单特殊来源标识：1省中，邵逸夫医保小程序; 默认null")
    private Integer recipeSource;

    @ItemProperty(alias = "CA密码")
    private String caPassword;

    @ItemProperty(alias = "药师处方签名生成的时间戳结构体，由院方服务器获取")
    private String signPharmacistCADate;

    @ItemProperty(alias = "医生处方数字签名值")
    private String signRecipeCode;

    @ItemProperty(alias = "医生处方签名生成的时间戳结构体，由院方服务器获取")
    private String signCADate;

    @ItemProperty(alias = "主诉")
    private String mainDieaseDescribe;

    @ItemProperty(alias = "处方来源类型 1 平台处方 2 线下转线上的处方")
    private Integer recipeSourceType;

    @ItemProperty(alias = "处方支付类型 0 普通支付 1 不选择购药方式直接去支付")
    private Integer recipePayType;

    private List<HisRecipeDetailBean> detailData;

    /**
     * 患者医保类型（编码）
     */
    private String medicalType;

    /**
     * 患者医保类型（名称）
     */
    private String medicalTypeText;

    @ItemProperty(alias = "过敏源")
    private List<AllergieBean> allergies;
    /**
     * 挂号序号
     */
    private String registerId;

    private Integer queryStatus;
    private String serialNumber;
    @ItemProperty(alias = "his处方付费序号合集")
    private String recipeCostNumber;

    @ItemProperty(alias = "是否被接方 0 未接方 1已接方")
    private Integer grabOrderStatus;

    @ItemProperty(alias = "his中药处方代煎费")
    private BigDecimal decoctionFee;

    public String getRecipeCostNumber() {
        return recipeCostNumber;
    }

    public void setRecipeCostNumber(String recipeCostNumber) {
        this.recipeCostNumber = recipeCostNumber;
    }
    /**
     * 电子病例是否更新 true 不更新
     */
    private Boolean emrStatus;

    public Integer getQueryStatus() {
        return queryStatus;
    }

    public void setQueryStatus(Integer queryStatus) {
        this.queryStatus = queryStatus;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public RecipeBean() {
    }

    public Integer getCheckStatus() {
        return checkStatus;
    }

    public void setCheckStatus(Integer checkStatus) {
        this.checkStatus = checkStatus;
    }

    /**
     * 处方单设置默认值
     */
    public void setDefaultData() {
        if (null == this.getRecipeType()) {
            this.setRecipeId(0);
        }

        //默认为西药
        if (null == this.getRecipeType()) {
            this.setRecipeType(1);
        }

        //默认剂数为1
        if (null == this.getCopyNum() || this.getCopyNum() < 1) {
            this.setCopyNum(1);
        }

        //默认无法医保支付
        if (null == this.getMedicalPayFlag()) {
            this.setMedicalPayFlag(0);
        }

        //默认可以医院，药企发药
        if (null == this.getDistributionFlag()) {
            this.setDistributionFlag(0);
        }

        //默认来源为纳里APP处方
        if (null == this.getFromflag()) {
            this.setFromflag(1);
        }

        //默认到院取药
        if (null == this.getGiveMode()) {
            this.setGiveMode(2);
        }

        //默认未签名
        if (null == this.getStatus()) {
            this.setStatus(0);
        }

        if (null == this.getCreateDate()) {
            Date now = new Date();
            this.setCreateDate(now);
            this.setLastModify(now);
        }

        //默认有效天数
        if (null == this.getValueDays()) {
            this.setValueDays(3);
        }

        //判断诊断备注是否为空，若为空则显示“无”
//        if (StringUtils.isEmpty(this.getMemo())) {
//            this.setMemo("无");
//        }

        if (null == this.getPayFlag()) {
            this.setPayFlag(0);
        }

        if (null == this.getChooseFlag()) {
            this.setChooseFlag(0);
        }

        if (null == this.getGiveFlag()) {
            this.setGiveFlag(0);
        }

        if (null == this.getRemindFlag()) {
            this.setRemindFlag(0);
        }

        if (null == this.getPushFlag()) {
            this.setPushFlag(0);
        }

        if (null == this.getTakeMedicine()) {
            this.setTakeMedicine(0);
        }

    }

    public String getRegisterId() {
        return registerId;
    }

    public void setRegisterId(String registerId) {
        this.registerId = registerId;
    }

    public String getRecipeIdE() {
        return recipeIdE;
    }

    public void setRecipeIdE(String recipeIdE) {
        this.recipeIdE = recipeIdE;
    }

    public Integer getReviewType() {
        return reviewType;
    }

    public void setReviewType(Integer reviewType) {
        this.reviewType = reviewType;
    }

    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public Integer getClinicId() {
        return clinicId;
    }

    public void setClinicId(Integer clinicId) {
        this.clinicId = clinicId;
    }

    public String getMpiid() {
        return mpiid;
    }

    public void setMpiid(String mpiid) {
        this.mpiid = mpiid;
    }

    public String getPatientID() {
        return patientID;
    }

    public void setPatientID(String patientID) {
        this.patientID = patientID;
    }

    public Integer getPatientStatus() {
        return patientStatus;
    }

    public void setPatientStatus(Integer patientStatus) {
        this.patientStatus = patientStatus;
    }

    public Integer getClinicOrgan() {
        return clinicOrgan;
    }

    public void setClinicOrgan(Integer clinicOrgan) {
        this.clinicOrgan = clinicOrgan;
    }

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public Integer getOriginClinicOrgan() {
        return originClinicOrgan;
    }

    public void setOriginClinicOrgan(Integer originClinicOrgan) {
        this.originClinicOrgan = originClinicOrgan;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public String getOriginRecipeCode() {
        return originRecipeCode;
    }

    public void setOriginRecipeCode(String originRecipeCode) {
        this.originRecipeCode = originRecipeCode;
    }

    public Integer getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(Integer recipeType) {
        this.recipeType = recipeType;
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

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Integer getCopyNum() {
        return copyNum;
    }

    public void setCopyNum(Integer copyNum) {
        this.copyNum = copyNum;
    }

    public BigDecimal getTotalMoney() {
        return totalMoney;
    }

    public void setTotalMoney(BigDecimal totalMoney) {
        this.totalMoney = totalMoney;
    }

    public String getOrganDiseaseName() {
        return organDiseaseName;
    }

    public void setOrganDiseaseName(String organDiseaseName) {
        this.organDiseaseName = organDiseaseName;
    }

    public String getOrganDiseaseId() {
        return organDiseaseId;
    }

    public void setOrganDiseaseId(String organDiseaseId) {
        this.organDiseaseId = organDiseaseId;
    }

    public Integer getPayFlag() {
        return payFlag;
    }

    public void setPayFlag(Integer payFlag) {
        this.payFlag = payFlag;
    }

    public Date getPayDate() {
        return payDate;
    }

    public void setPayDate(Date payDate) {
        this.payDate = payDate;
    }

    public Integer getPayListId() {
        return payListId;
    }

    public void setPayListId(Integer payListId) {
        this.payListId = payListId;
    }

    public Integer getGiveOrgan() {
        return giveOrgan;
    }

    public void setGiveOrgan(Integer giveOrgan) {
        this.giveOrgan = giveOrgan;
    }

    public Integer getGiveFlag() {
        return giveFlag;
    }

    public String getGiveModeText() { return giveModeText; }

    public void setGiveModeText(String giveModeText) { this.giveModeText = giveModeText; }

    public void setGiveFlag(Integer giveFlag) {
        this.giveFlag = giveFlag;
    }

    public Date getGiveDate() {
        return giveDate;
    }

    public void setGiveDate(Date giveDate) {
        this.giveDate = giveDate;
    }

    public Integer getValueDays() {
        return valueDays;
    }

    public void setValueDays(Integer valueDays) {
        this.valueDays = valueDays;
    }

    public Integer getCheckOrgan() {
        return checkOrgan;
    }

    public void setCheckOrgan(Integer checkOrgan) {
        this.checkOrgan = checkOrgan;
    }

    public Date getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(Date checkDate) {
        this.checkDate = checkDate;
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

    public String getCheckerTel() {
        return checkerTel;
    }

    public void setCheckerTel(String checkerTel) {
        this.checkerTel = checkerTel;
    }

    public Integer getPayMode() {
        return payMode;
    }

    public void setPayMode(Integer payMode) {
        this.payMode = payMode;
    }

    public Integer getGiveMode() {
        return giveMode;
    }

    public void setGiveMode(Integer giveMode) {
        this.giveMode = giveMode;
    }

    public String getGiveUser() {
        return giveUser;
    }

    public void setGiveUser(String giveUser) {
        this.giveUser = giveUser;
    }

    public String getSignFile() {
        return signFile;
    }

    public void setSignFile(String signFile) {
        this.signFile = signFile;
    }

    public String getChemistSignFile() {
        return chemistSignFile;
    }

    public void setChemistSignFile(String chemistSignFile) {
        this.chemistSignFile = chemistSignFile;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getRecMobile() {
        return recMobile;
    }

    public void setRecMobile(String recMobile) {
        this.recMobile = recMobile;
    }

    public String getRecTel() {
        return recTel;
    }

    public void setRecTel(String recTel) {
        this.recTel = recTel;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getAddress3() {
        return address3;
    }

    public void setAddress3(String address3) {
        this.address3 = address3;
    }

    public String getAddress4() {
        return address4;
    }

    public void setAddress4(String address4) {
        this.address4 = address4;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public Integer getAddressId() {
        return addressId;
    }

    public void setAddressId(Integer addressId) {
        this.addressId = addressId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getFromflag() {
        return fromflag;
    }

    public void setFromflag(Integer fromflag) {
        this.fromflag = fromflag;
    }

    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    public Date getStartSendDate() {
        return startSendDate;
    }

    public void setStartSendDate(Date startSendDate) {
        this.startSendDate = startSendDate;
    }

    public Date getSendDate() {
        return sendDate;
    }

    public void setSendDate(Date sendDate) {
        this.sendDate = sendDate;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public Date getSignDate() {
        return signDate;
    }

    public void setSignDate(Date signDate) {
        this.signDate = signDate;
    }

    public String getRecipeDrugName() {
        return recipeDrugName;
    }

    public void setRecipeDrugName(String recipeDrugName) {
        this.recipeDrugName = recipeDrugName;
    }

    public Date getRecipeShowTime() {
        return recipeShowTime;
    }

    public void setRecipeShowTime(Date recipeShowTime) {
        this.recipeShowTime = recipeShowTime;
    }

    public String getRecipeSurplusHours() {
        return recipeSurplusHours;
    }

    public void setRecipeSurplusHours(String recipeSurplusHours) {
        this.recipeSurplusHours = recipeSurplusHours;
    }

    public String getCheckFailMemo() {
        return checkFailMemo;
    }

    public void setCheckFailMemo(String checkFailMemo) {
        this.checkFailMemo = checkFailMemo;
    }

    public String getSupplementaryMemo() {
        return supplementaryMemo;
    }

    public void setSupplementaryMemo(String supplementaryMemo) {
        this.supplementaryMemo = supplementaryMemo;
    }

    public Integer getChooseFlag() {
        return chooseFlag;
    }

    public void setChooseFlag(Integer chooseFlag) {
        this.chooseFlag = chooseFlag;
    }

    public Integer getRemindFlag() {
        return remindFlag;
    }

    public void setRemindFlag(Integer remindFlag) {
        this.remindFlag = remindFlag;
    }

    public String getTradeNo() {
        return tradeNo;
    }

    public void setTradeNo(String tradeNo) {
        this.tradeNo = tradeNo;
    }

    public String getWxPayWay() {
        return wxPayWay;
    }

    public void setWxPayWay(String wxPayWay) {
        this.wxPayWay = wxPayWay;
    }

    public String getOutTradeNo() {
        return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo) {
        this.outTradeNo = outTradeNo;
    }

    public String getPayOrganId() {
        return payOrganId;
    }

    public void setPayOrganId(String payOrganId) {
        this.payOrganId = payOrganId;
    }

    public String getWxPayErrorCode() {
        return wxPayErrorCode;
    }

    public void setWxPayErrorCode(String wxPayErrorCode) {
        this.wxPayErrorCode = wxPayErrorCode;
    }

    public Integer getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(Integer enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public Integer getPushFlag() {
        return pushFlag;
    }

    public void setPushFlag(Integer pushFlag) {
        this.pushFlag = pushFlag;
    }

    public Integer getOldRecipeId() {
        return oldRecipeId;
    }

    public void setOldRecipeId(Integer oldRecipeId) {
        this.oldRecipeId = oldRecipeId;
    }

    public Integer getCouponId() {
        return couponId;
    }

    public void setCouponId(Integer couponId) {
        this.couponId = couponId;
    }

    public BigDecimal getActualPrice() {
        return actualPrice;
    }

    public void setActualPrice(BigDecimal actualPrice) {
        this.actualPrice = actualPrice;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }

    public String getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(String discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Integer getMedicalPayFlag() {
        return medicalPayFlag;
    }

    public void setMedicalPayFlag(Integer medicalPayFlag) {
        this.medicalPayFlag = medicalPayFlag;
    }

    public Integer getDistributionFlag() {
        return distributionFlag;
    }

    public void setDistributionFlag(Integer distributionFlag) {
        this.distributionFlag = distributionFlag;
    }

    public String getRecipeMemo() {
        return recipeMemo;
    }

    public void setRecipeMemo(String recipeMemo) {
        this.recipeMemo = recipeMemo;
    }

    public String getTcmUsePathways() {
        return tcmUsePathways;
    }

    public void setTcmUsePathways(String tcmUsePathways) {
        this.tcmUsePathways = tcmUsePathways;
    }

    public String getTcmUsingRate() {
        return tcmUsingRate;
    }

    public void setTcmUsingRate(String tcmUsingRate) {
        this.tcmUsingRate = tcmUsingRate;
    }

    public String getShowTip() {
        return showTip;
    }

    public void setShowTip(String showTip) {
        this.showTip = showTip;
    }

    public BigDecimal getPrice1() {
        return price1;
    }

    public void setPrice1(BigDecimal price1) {
        this.price1 = price1;
    }

    public BigDecimal getPrice2() {
        return price2;
    }

    public void setPrice2(BigDecimal price2) {
        this.price2 = price2;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Integer getTakeMedicine() {
        return takeMedicine;
    }

    public void setTakeMedicine(Integer takeMedicine) {
        this.takeMedicine = takeMedicine;
    }

    public String getRequestMpiId() {
        return requestMpiId;
    }

    public void setRequestMpiId(String requestMpiId) {
        this.requestMpiId = requestMpiId;
    }

    public Integer getRequestUrt() {
        return requestUrt;
    }

    public void setRequestUrt(Integer requestUrt) {
        this.requestUrt = requestUrt;
    }

    public Integer getCurrentClient() {
        return currentClient;
    }

    public void setCurrentClient(Integer currentClient) {
        this.currentClient = currentClient;
    }

    public int getNotation() {
        return notation;
    }

    public void setNotation(int notation) {
        this.notation = notation;
    }

    public RecipeExtendBean getRecipeExtend() {
        return recipeExtend;
    }

    public void setRecipeExtend(RecipeExtendBean recipeExtend) {
        this.recipeExtend = recipeExtend;
    }

    public String getRecipeMode() {
        return recipeMode;
    }

    public void setRecipeMode(String recipeMode) {
        this.recipeMode = recipeMode;
    }

    public boolean canMedicalPay() {
        Integer useMedicalFlag = 1;
        return (useMedicalFlag.equals(medicalPayFlag)) ? true : false;
    }

    public Integer getBussSource() {
        return bussSource;
    }

    public void setBussSource(Integer bussSource) {
        this.bussSource = bussSource;
    }

    public Integer getRecipeSource() {
        return recipeSource;
    }

    public void setRecipeSource(Integer recipeSource) {
        this.recipeSource = recipeSource;
    }

    public String getCaPassword() {
        return caPassword;
    }

    public void setCaPassword(String caPassword) {
        this.caPassword = caPassword;
    }

    public Integer getCheckMode() {
        return checkMode;
    }

    public void setCheckMode(Integer checkMode) {
        this.checkMode = checkMode;
    }

    public String getSignPharmacistCADate() { return signPharmacistCADate; }

    public void setSignPharmacistCADate(String signPharmacistCADate) { this.signPharmacistCADate = signPharmacistCADate; }

    public String getSignRecipeCode() {
        return signRecipeCode;
    }

    public void setSignRecipeCode(String signRecipeCode) {
        this.signRecipeCode = signRecipeCode;
    }

    public String getSignCADate() {
        return signCADate;
    }

    public void setSignCADate(String signCADate) {
        this.signCADate = signCADate;
    }

    public String getMainDieaseDescribe() {
        return mainDieaseDescribe;
    }

    public void setMainDieaseDescribe(String mainDieaseDescribe) {
        this.mainDieaseDescribe = mainDieaseDescribe;
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

    public Integer getRecipeSourceType() {
        return recipeSourceType;
    }

    public void setRecipeSourceType(Integer recipeSourceType) {
        this.recipeSourceType = recipeSourceType;
    }

    public Integer getRecipePayType() {
        return recipePayType;
    }

    public void setRecipePayType(Integer recipePayType) {
        this.recipePayType = recipePayType;
    }

    public List<HisRecipeDetailBean> getDetailData() {
        return detailData;
    }

    public void setDetailData(List<HisRecipeDetailBean> detailData) {
        this.detailData = detailData;
    }

    public List<AllergieBean> getAllergies() {
        return allergies;
    }

    public void setAllergies(List<AllergieBean> allergies) {
        this.allergies = allergies;
    }

    public Boolean getEmrStatus() {
        return emrStatus;
    }

    public void setEmrStatus(Boolean emrStatus) {
        this.emrStatus = emrStatus;
    }

    public String getCheckerText() {
        return checkerText;
    }

    public void setCheckerText(String checkerText) {
        this.checkerText = checkerText;
    }

    public Integer getGrabOrderStatus() {
        return grabOrderStatus;
    }

    public void setGrabOrderStatus(Integer grabOrderStatus) {
        this.grabOrderStatus = grabOrderStatus;
    }

    public BigDecimal getDecoctionFee() {
        return decoctionFee;
    }

    public void setDecoctionFee(BigDecimal decoctionFee) {
        this.decoctionFee = decoctionFee;
    }
}
