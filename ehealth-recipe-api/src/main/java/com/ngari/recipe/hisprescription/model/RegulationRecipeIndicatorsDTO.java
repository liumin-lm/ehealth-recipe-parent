package com.ngari.recipe.hisprescription.model;

import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 处方业务指标
 */
public class RegulationRecipeIndicatorsDTO implements Serializable {


    private static final long serialVersionUID = 2237309233285181635L;
    private String organID;//	机构唯一号
    private String organizeCode;//机构标识
    private String organName;
    private String hosCode;//	院区代码
    private String hosName;//	院区名称
    private String mpiId;
    private String bussID;//	互联网医院复诊记录Id
    private String bussSource;// 处方来源标记   1 问诊  4 复诊
    private String patientNumber;//门诊号
    private String originalDiagnosis;//上次就诊诊断名称
    private String subjectCode;//	开方医师所属专业代码（诊疗科目代码）
    private String subjectName;//	开方医师所属专业名称（诊疗科目名称）
    private String deptID;//	医师所属科室代码
    private String deptName;//	医师所属科室名称
    private String doctorId;//	医生ID
    @Desensitizations(type = DesensitizationsType.IDCARD)
    private String doctorCertID;//	医师身份证号
    private String doctorName;//	医师姓名
    private String doctorNo;// 开方医生工号
    private String doctorProTitle;//开方医生职称
    private String auditDoctorId;//	审方医生ID
    @Desensitizations(type = DesensitizationsType.IDCARD)
    private String auditDoctorCertID;//	审方医师身份证号
    private String auditDoctor;//	审方医师姓名
    private String auditDoctorNo;// 审方医生工号
    private String auditDoctorSign;//审核医生电子签名
    private String auditProTitle;//审方医生职称
    private String auditDeptID;//审方医师所属科室代码
    private String auditDeptName;//审方医师所属科室名称
    private String auditSubjectCode;//	审方医师所属专业代码（诊疗科目代码）
    private String auditSubjectName;//	审方医师所属专业名称（诊疗科目名称）
    private String patientCardType;//	患者证件
    @Desensitizations(type = DesensitizationsType.IDCARD)
    private String patientCertID;//	患者证件号
    private String patientName;//	患者姓名
    private Integer age;//	患者年龄
    private String sex;//	患者性别
    private Date birthDay;//患者出生日期
    @Desensitizations(type = DesensitizationsType.MOBILE)
    private String mobile;//	患者联系电话
    private String costType;//	费别 1自费 2医保
    private String cardType;//	卡类型
    @Desensitizations(type = DesensitizationsType.IDCARD)
    private String cardNo;//	卡号
    @Desensitizations(type = DesensitizationsType.IDCARD)
    private String guardianCertID;
    private String guardianName;
    private String guardianMobile;

    private String allergyFlag;//过敏史标记0:无 1:有
    private String allergyInfo;//	过敏信息
    private String diseasesHistory;//	患者简要病史描述
    private String recipeUniqueID;//	互联网医院处方唯一号
    private String recipeID;//	互联网医院处方号
    private String recipeRecordNo;//处方医保备案号
    private String rationalFlag;//是否经过合理用药判断标志 1经过0未经过
    private String rationalDrug;//	合理用药审核结果
    private String CAInfo;//	处方CA认证文件信息
    private String recipeFileId; // 处方笺文件id
    private String medicalFileId; //患者病历文件id
    private String icdCode;//	诊断ICD码
    private String icdName;//	初步诊断名称
    private String memo;//诊断备注
    private String recipeType;//	处方类型 1西药 2成药 3草药
    private Integer packetsNum;//	帖数
    private Date datein;//	处方日期
    private Integer effectivePeriod;//	处方效期
    private Date startDate;//	处方开始日期
    private Date endDate;//	处方结束日期
    private Date checkDate;//处方审核时间
    private String deliveryType;//	处方配送方式 0医院取药 1物流配送 2药店取药
    private String deliveryFirm;//	配送厂商名称
    private Date deliveryDate;//	配送时间
    private Date sendTime; //配送开始时间
    private Date finishTime; //配送结束时间
    private Integer deliveryStatus; //配送状态
    private Double totalFee;//	处方金额
    private String isPay;//	是否支付
    private String tradeNo;//	第三方支付交易流水号
    private String verificationStatus;//	处方核销状态 0未核销 1已核销
    private Date verificationTime;//	处方核销时间
    private String verificationUnit;//	处方核销单位
    private Date updateTime;//	最后更新时间

    private Integer recipeStatus;//处方状态

    private String payFlag; //支付标识 0未支付 1已支付

    private String signRecipeCode; //医生处方数字签名值

    private String signPharmacistCode; //药师处方数字签名值

    private String signCADate; //可信时间戳（医生处方签名生成时间戳）

    private String serialNumCA; //医护人员证件序列号

//    private String  satisfaction; //满意度
//    private String  scoring;//评分
//    private String  evaluation;//评价内容
//    private String  complaints;//投诉建议

    private List<RegulationRecipeDetailIndicatorsReq> orderList;//处方列表数据集

    private String unitID;

    private String cancelFlag;//撤销标记 1-正常 2-撤销

    private String doctorSign;//开方医生电子签名

    private String mainDieaseDescribe;//主诉

    private String currentMedical;//现病史

    private String histroyMedical;//既往史

    private Date consultStartDate;//咨询开始日期

    private FirstVisitRecord firstVisitRecord;//初诊列表数据集

    //发药药师姓名
    private String dispensingApothecaryName;
    //发药药师身份证号
    @Desensitizations(type = DesensitizationsType.IDCARD)
    private String dispensingApothecaryIdCard;

    private String einvoiceNumber; // 电子发票号

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 支付时间
     */
    private Date payTime;

    public String getOutTradeNo() {
        return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo) {
        this.outTradeNo = outTradeNo;
    }

    public Date getPayTime() {
        return payTime;
    }

    public void setPayTime(Date payTime) {
        this.payTime = payTime;
    }

    public String getUnitID() {
        return unitID;
    }

    public void setUnitID(String unitID) {
        this.unitID = unitID;
    }

    public String getOrganID() {
        return organID;
    }

    public void setOrganID(String organID) {
        this.organID = organID;
    }

    public String getHosCode() {
        return hosCode;
    }

    public void setHosCode(String hosCode) {
        this.hosCode = hosCode;
    }

    public String getHosName() {
        return hosName;
    }

    public void setHosName(String hosName) {
        this.hosName = hosName;
    }

    public String getBussID() {
        return bussID;
    }

    public void setBussID(String bussID) {
        this.bussID = bussID;
    }

    public String getOriginalDiagnosis() {
        return originalDiagnosis;
    }

    public void setOriginalDiagnosis(String originalDiagnosis) {
        this.originalDiagnosis = originalDiagnosis;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getDeptID() {
        return deptID;
    }

    public void setDeptID(String deptID) {
        this.deptID = deptID;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorCertID() {
        return doctorCertID;
    }

    public void setDoctorCertID(String doctorCertID) {
        this.doctorCertID = doctorCertID;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getAuditDoctorId() {
        return auditDoctorId;
    }

    public void setAuditDoctorId(String auditDoctorId) {
        this.auditDoctorId = auditDoctorId;
    }

    public String getAuditDoctorCertID() {
        return auditDoctorCertID;
    }

    public void setAuditDoctorCertID(String auditDoctorCertID) {
        this.auditDoctorCertID = auditDoctorCertID;
    }

    public String getAuditDoctor() {
        return auditDoctor;
    }

    public void setAuditDoctor(String auditDoctor) {
        this.auditDoctor = auditDoctor;
    }

    public String getPatientCardType() {
        return patientCardType;
    }

    public void setPatientCardType(String patientCardType) {
        this.patientCardType = patientCardType;
    }

    public String getPatientCertID() {
        return patientCertID;
    }

    public void setPatientCertID(String patientCertID) {
        this.patientCertID = patientCertID;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getCostType() {
        return costType;
    }

    public void setCostType(String costType) {
        this.costType = costType;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public String getGuardianCertID() {
        return guardianCertID;
    }

    public void setGuardianCertID(String guardianCertID) {
        this.guardianCertID = guardianCertID;
    }

    public String getGuardianName() {
        return guardianName;
    }

    public void setGuardianName(String guardianName) {
        this.guardianName = guardianName;
    }

    public String getAllergyInfo() {
        return allergyInfo;
    }

    public void setAllergyInfo(String allergyInfo) {
        this.allergyInfo = allergyInfo;
    }

    public String getDiseasesHistory() {
        return diseasesHistory;
    }

    public void setDiseasesHistory(String diseasesHistory) {
        this.diseasesHistory = diseasesHistory;
    }

    public String getRecipeUniqueID() {
        return recipeUniqueID;
    }

    public void setRecipeUniqueID(String recipeUniqueID) {
        this.recipeUniqueID = recipeUniqueID;
    }

    public String getRecipeID() {
        return recipeID;
    }

    public void setRecipeID(String recipeID) {
        this.recipeID = recipeID;
    }

    public String getRationalDrug() {
        return rationalDrug;
    }

    public void setRationalDrug(String rationalDrug) {
        this.rationalDrug = rationalDrug;
    }

    public String getCAInfo() {
        return CAInfo;
    }

    public void setCAInfo(String CAInfo) {
        this.CAInfo = CAInfo;
    }

    public String getIcdCode() {
        return icdCode;
    }

    public void setIcdCode(String icdCode) {
        this.icdCode = icdCode;
    }

    public String getIcdName() {
        return icdName;
    }

    public void setIcdName(String icdName) {
        this.icdName = icdName;
    }

    public String getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(String recipeType) {
        this.recipeType = recipeType;
    }

    public Integer getPacketsNum() {
        return packetsNum;
    }

    public void setPacketsNum(Integer packetsNum) {
        this.packetsNum = packetsNum;
    }

    public Date getDatein() {
        return datein;
    }

    public void setDatein(Date datein) {
        this.datein = datein;
    }

    public Integer getEffectivePeriod() {
        return effectivePeriod;
    }

    public void setEffectivePeriod(Integer effectivePeriod) {
        this.effectivePeriod = effectivePeriod;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(String deliveryType) {
        this.deliveryType = deliveryType;
    }

    public String getDeliveryFirm() {
        return deliveryFirm;
    }

    public void setDeliveryFirm(String deliveryFirm) {
        this.deliveryFirm = deliveryFirm;
    }

    public Date getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(Date deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public Double getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(Double totalFee) {
        this.totalFee = totalFee;
    }

    public String getIsPay() {
        return isPay;
    }

    public void setIsPay(String isPay) {
        this.isPay = isPay;
    }

    public String getTradeNo() {
        return tradeNo;
    }

    public void setTradeNo(String tradeNo) {
        this.tradeNo = tradeNo;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Date getVerificationTime() {
        return verificationTime;
    }

    public void setVerificationTime(Date verificationTime) {
        this.verificationTime = verificationTime;
    }

    public String getVerificationUnit() {
        return verificationUnit;
    }

    public void setVerificationUnit(String verificationUnit) {
        this.verificationUnit = verificationUnit;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public List<RegulationRecipeDetailIndicatorsReq> getOrderList() {
        return orderList;
    }

    public void setOrderList(List<RegulationRecipeDetailIndicatorsReq> orderList) {
        this.orderList = orderList;
    }

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public String getRecipeRecordNo() {
        return recipeRecordNo;
    }

    public void setRecipeRecordNo(String recipeRecordNo) {
        this.recipeRecordNo = recipeRecordNo;
    }

    public String getRationalFlag() {
        return rationalFlag;
    }

    public void setRationalFlag(String rationalFlag) {
        this.rationalFlag = rationalFlag;
    }

    public String getPatientNumber() {
        return patientNumber;
    }

    public void setPatientNumber(String patientNumber) {
        this.patientNumber = patientNumber;
    }

    public String getDoctorNo() {
        return doctorNo;
    }

    public void setDoctorNo(String doctorNo) {
        this.doctorNo = doctorNo;
    }

    public String getAuditDoctorNo() {
        return auditDoctorNo;
    }

    public void setAuditDoctorNo(String auditDoctorNo) {
        this.auditDoctorNo = auditDoctorNo;
    }

    public Date getBirthDay() {
        return birthDay;
    }

    public void setBirthDay(Date birthDay) {
        this.birthDay = birthDay;
    }

    public String getAllergyFlag() {
        return allergyFlag;
    }

    public void setAllergyFlag(String allergyFlag) {
        this.allergyFlag = allergyFlag;
    }

    public String getOrganizeCode() {
        return organizeCode;
    }

    public void setOrganizeCode(String organizeCode) {
        this.organizeCode = organizeCode;
    }

    public String getCancelFlag() {
        return cancelFlag;
    }

    public void setCancelFlag(String cancelFlag) {
        this.cancelFlag = cancelFlag;
    }

    public String getDoctorSign() {
        return doctorSign;
    }

    public void setDoctorSign(String doctorSign) {
        this.doctorSign = doctorSign;
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

    public Date getConsultStartDate() {
        return consultStartDate;
    }

    public void setConsultStartDate(Date consultStartDate) {
        this.consultStartDate = consultStartDate;
    }

    public FirstVisitRecord getFirstVisitRecord() {
        return firstVisitRecord;
    }

    public void setFirstVisitRecord(FirstVisitRecord firstVisitRecord) {
        this.firstVisitRecord = firstVisitRecord;
    }

    public Integer getRecipeStatus() {
        return recipeStatus;
    }

    public void setRecipeStatus(Integer recipeStatus) {
        this.recipeStatus = recipeStatus;
    }

    public String getPayFlag() {
        return payFlag;
    }

    public void setPayFlag(String payFlag) {
        this.payFlag = payFlag;
    }

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

    public Date getSendTime() {
        return sendTime;
    }

    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    public Integer getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(Integer deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public String getDispensingApothecaryName() {
        return dispensingApothecaryName;
    }

    public void setDispensingApothecaryName(String dispensingApothecaryName) {
        this.dispensingApothecaryName = dispensingApothecaryName;
    }

    public String getDispensingApothecaryIdCard() {
        return dispensingApothecaryIdCard;
    }

    public void setDispensingApothecaryIdCard(String dispensingApothecaryIdCard) {
        this.dispensingApothecaryIdCard = dispensingApothecaryIdCard;
    }

    public String getMpiId() {
        return mpiId;
    }

    public void setMpiId(String mpiId) {
        this.mpiId = mpiId;
    }

    public String getBussSource() {
        return bussSource;
    }

    public void setBussSource(String bussSource) {
        this.bussSource = bussSource;
    }

    public String getDoctorProTitle() {
        return doctorProTitle;
    }

    public void setDoctorProTitle(String doctorProTitle) {
        this.doctorProTitle = doctorProTitle;
    }

    public String getAuditDoctorSign() {
        return auditDoctorSign;
    }

    public void setAuditDoctorSign(String auditDoctorSign) {
        this.auditDoctorSign = auditDoctorSign;
    }

    public String getAuditProTitle() {
        return auditProTitle;
    }

    public void setAuditProTitle(String auditProTitle) {
        this.auditProTitle = auditProTitle;
    }

    public String getAuditDeptID() {
        return auditDeptID;
    }

    public void setAuditDeptID(String auditDeptID) {
        this.auditDeptID = auditDeptID;
    }

    public String getAuditDeptName() {
        return auditDeptName;
    }

    public void setAuditDeptName(String auditDeptName) {
        this.auditDeptName = auditDeptName;
    }

    public String getAuditSubjectCode() {
        return auditSubjectCode;
    }

    public void setAuditSubjectCode(String auditSubjectCode) {
        this.auditSubjectCode = auditSubjectCode;
    }

    public String getAuditSubjectName() {
        return auditSubjectName;
    }

    public void setAuditSubjectName(String auditSubjectName) {
        this.auditSubjectName = auditSubjectName;
    }

    public String getGuardianMobile() {
        return guardianMobile;
    }

    public void setGuardianMobile(String guardianMobile) {
        this.guardianMobile = guardianMobile;
    }

    public String getRecipeFileId() {
        return recipeFileId;
    }

    public void setRecipeFileId(String recipeFileId) {
        this.recipeFileId = recipeFileId;
    }

    public String getMedicalFileId() {
        return medicalFileId;
    }

    public void setMedicalFileId(String medicalFileId) {
        this.medicalFileId = medicalFileId;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Date getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(Date checkDate) {
        this.checkDate = checkDate;
    }

    public String getSignPharmacistCode() {
        return signPharmacistCode;
    }

    public void setSignPharmacistCode(String signPharmacistCode) {
        this.signPharmacistCode = signPharmacistCode;
    }

    public String getSerialNumCA() {
        return serialNumCA;
    }

    public void setSerialNumCA(String serialNumCA) {
        this.serialNumCA = serialNumCA;
    }

    public String getEinvoiceNumber() {
        return einvoiceNumber;
    }

    public void setEinvoiceNumber(String einvoiceNumber) {
        this.einvoiceNumber = einvoiceNumber;
    }
}
