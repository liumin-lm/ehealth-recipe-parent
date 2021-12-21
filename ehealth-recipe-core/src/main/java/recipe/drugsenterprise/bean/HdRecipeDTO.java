package recipe.drugsenterprise.bean;

import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: 对接华东处方推送的中间对象
 * 不用脱敏 对接第三方
 * @Author: JRK
 * @Date: 2019/7/23
 */
@Schema
public class HdRecipeDTO implements Serializable {
    private static final long serialVersionUID = 1090635451107957887L;
    /**
     * 标识来源平台[纳里：10固定]
     */
    private String sourceId;
    /**
     * 门店机构编码
     * 药店下的code
     */
    private String pharmacyCode;
    /**
     * 患者的证件类型
     * recipe下Mpiid对应的patientCertificateType
     * 对接系统中的字典
     */
    private String certificateType;
    /**
     * 患者证件号
     * recipe下Mpiid对应的patientCertificate
     */
    private String certificate;
    /**
     * 患者电话
     * recipe下Mpiid对应的Mobile
     */
    private String patientTelpatient;
    /**
     * 患者省信息
     * 非必填
     */
    private String Province;
    /**
     * 患者市信息
     * 非必填
     */
    private String City;
    /**
     * 患者区信息
     * 非必填
     */
    private String District;
    /**
     * 患者街道信息
     * 非必填
     */
    private String Street;
    /**
     * 患者地址信息
     * 非必填
     */
    private String Address;
    /**
     * 患者配送地址信息
     * 非必填
     */
    private String patientAddress;

    //收件人姓名
    private String recipientName;

    //收件人电话
    private String recipientTel;
    /**
     * 患者医院病历号
     * 非必填
     */
    private String patientNumber;
    /**
     * 电子处方单号
     * 处方标识recipecode
     */
    private String recipeCode;
    /**
     * 电子处方id
     * 处方标识recipeId
     */
    private String recipeId;
    /**
     * 处方类型
     * recipe的RecipeType
     * 对接系统的字典
     */
    private String recipeType;
    /**
     * 科别
     * recipe的Depart科室的id
     */
    private String departId;
    /**
     * 科别名称
     * recipe的Depart科室的name
     */
    private String departName;
    /**
     * 开方医生工号
     * recipe的doctor的name
     */
    private String doctorNumber;
    /**
     * 医生名称
     * recipe下doctor的name
     */
    private String doctorName;


    /**
     * 是否为杭州市医保
     * 1: 医保支付 0: 其他
     * 默认
     */
    private String medicalPay;
    /**
     * 处方日期
     * 例如：开方时间[yyyy-mm-dd h24:min:ss]
     * 处方签名时间
     */
    private String createDate;
    /**
     * 总金额
     * recipe.totalMoney
     */
    private String recipeFee;
    /**
     * 实际支付金额
     * 这个怎么区别
     */
    private String actualFee;
    /**
     * 优惠金额
     * 这个怎么区别
     */
    private String couponFee;
    /**
     * 待煎费或者膏方制作
     * 非必填
     */
    private String decoctionFee;
    /**
     * 医保报销金额
     * 非必填
     * 1: 医保支付 0: 其他
     */
    private String medicalFee;
    /**
     * 配送费
     * 非必填
     */
    private String expressFee;
    /**
     * 订单总价
     * 这里是药店金额？
     */
    private String orderTotalFee;
    /**
     * 诊断疾病名称
     * recipe的organDiseaseName
     */
    private String organDiseaseName;
    /**
     * 诊断疾病编码
     * recipe的organDiseaseId
     */
    private String organDiseaseId;
    /**
     * 诊断备注
     * 非必填
     */
    private String memo;

    /**
     * 医院编码
     * recipe下clinicOrgan对应的OrganCode
     * 非必填
     */
    private String organCode;

    /**
     * 医院编码
     * recipe下clinicOrgan对应的OrganName
     * 非必填
     */
    private String organName;
    /**
     * 支付方式
     * 1线上支付
     * 2货到付款
     * 非必填
     */
    private String payMode;
    /**
     * 是否已支付
     * 非必填
     * 1: 已支付 0: 未支付
     */
    private String payFlag;
    /**
     * 发药方式，参照[发药方式]对应字典
     * 非必填
     */
    private String giveMode;
    /**
     * 发药人姓名
     * 非必填
     */
    private String giveUser;

    /**
     * 病人名称
     * recipe下Mpiid对应的patientName
     */
    private String patientName;
    /**
     * 是否只走配送
     * 1: 只走配送 0: 其他
     */
    private String distributionFlag;
    /**
     * 处方备注
     * 非必填
     */
    private String recipeMemo;
    /**
     * 中药处方用法
     * 非必填
     */
    private String tcmUsePathways;
    /**
     * 中药处方用量
     * 非必填
     */
    private String tcmUsingRate;
    /**
     * 帖数
     * 非必填
     */
    private String tcmNum;
    /**
     * 配送厂商编码
     * 非必填
     */
    private String distributorCode;
    /**
     * 配送厂商名称
     * 非必填
     */
    private String distributorName;
    /**
     * 审核意见
     * 非必填
     */
    private String attitude;
    /**
     * 审核药师
     * recipe的checker
     */
    private String auditor;
    /**
     * 审核时间[yyyy-mm-dd h24:min:ss]
     */
    private String audiDate;
    /**
     * 预计审核时间
     * 非必填
     */
    private String planDate;
    /**
     * 处方单BASE64信息,数组转字符串，并压缩
     */
    private String image;

    /**
     * 商品明细
     */
    private List<HdDrugDTO> drugList;

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getPharmacyCode() {
        return pharmacyCode;
    }

    public void setPharmacyCode(String pharmacyCode) {
        this.pharmacyCode = pharmacyCode;
    }

    public String getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(String certificateType) {
        this.certificateType = certificateType;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getPatientTelpatient() {
        return patientTelpatient;
    }

    public void setPatientTelpatient(String patientTelpatient) {
        this.patientTelpatient = patientTelpatient;
    }

    public String getProvince() {
        return Province;
    }

    public void setProvince(String province) {
        Province = province;
    }

    public String getCity() {
        return City;
    }

    public void setCity(String city) {
        City = city;
    }

    public String getDistrict() {
        return District;
    }

    public void setDistrict(String district) {
        District = district;
    }

    public String getStreet() {
        return Street;
    }

    public void setStreet(String street) {
        Street = street;
    }

    public String getAddress() {
        return Address;
    }

    public void setAddress(String address) {
        Address = address;
    }

    public String getPatientAddress() {
        return patientAddress;
    }

    public void setPatientAddress(String patientAddress) {
        this.patientAddress = patientAddress;
    }

    public String getPatientNumber() {
        return patientNumber;
    }

    public void setPatientNumber(String patientNumber) {
        this.patientNumber = patientNumber;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(String recipeType) {
        this.recipeType = recipeType;
    }

    public String getDepartId() {
        return departId;
    }

    public void setDepartId(String departId) {
        this.departId = departId;
    }

    public String getDepartName() {
        return departName;
    }

    public void setDepartName(String departName) {
        this.departName = departName;
    }

    public String getDoctorNumber() {
        return doctorNumber;
    }

    public void setDoctorNumber(String doctorNumber) {
        this.doctorNumber = doctorNumber;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getMedicalPay() {
        return medicalPay;
    }

    public void setMedicalPay(String medicalPay) {
        this.medicalPay = medicalPay;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getRecipeFee() {
        return recipeFee;
    }

    public void setRecipeFee(String recipeFee) {
        this.recipeFee = recipeFee;
    }

    public String getActualFee() {
        return actualFee;
    }

    public void setActualFee(String actualFee) {
        this.actualFee = actualFee;
    }

    public String getCouponFee() {
        return couponFee;
    }

    public void setCouponFee(String couponFee) {
        this.couponFee = couponFee;
    }

    public String getDecoctionFee() {
        return decoctionFee;
    }

    public void setDecoctionFee(String decoctionFee) {
        this.decoctionFee = decoctionFee;
    }

    public String getMedicalFee() {
        return medicalFee;
    }

    public void setMedicalFee(String medicalFee) {
        this.medicalFee = medicalFee;
    }

    public String getExpressFee() {
        return expressFee;
    }

    public void setExpressFee(String expressFee) {
        this.expressFee = expressFee;
    }

    public String getOrderTotalFee() {
        return orderTotalFee;
    }

    public void setOrderTotalFee(String orderTotalFee) {
        this.orderTotalFee = orderTotalFee;
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

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getOrganCode() {
        return organCode;
    }

    public void setOrganCode(String organCode) {
        this.organCode = organCode;
    }

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public String getPayMode() {
        return payMode;
    }

    public void setPayMode(String payMode) {
        this.payMode = payMode;
    }

    public String getPayFlag() {
        return payFlag;
    }

    public void setPayFlag(String payFlag) {
        this.payFlag = payFlag;
    }

    public String getGiveMode() {
        return giveMode;
    }

    public void setGiveMode(String giveMode) {
        this.giveMode = giveMode;
    }

    public String getGiveUser() {
        return giveUser;
    }

    public void setGiveUser(String giveUser) {
        this.giveUser = giveUser;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDistributionFlag() {
        return distributionFlag;
    }

    public void setDistributionFlag(String distributionFlag) {
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

    public String getTcmNum() {
        return tcmNum;
    }

    public void setTcmNum(String tcmNum) {
        this.tcmNum = tcmNum;
    }

    public String getDistributorCode() {
        return distributorCode;
    }

    public void setDistributorCode(String distributorCode) {
        this.distributorCode = distributorCode;
    }

    public String getDistributorName() {
        return distributorName;
    }

    public void setDistributorName(String distributorName) {
        this.distributorName = distributorName;
    }

    public String getAttitude() {
        return attitude;
    }

    public void setAttitude(String attitude) {
        this.attitude = attitude;
    }

    public String getAuditor() {
        return auditor;
    }

    public void setAuditor(String auditor) {
        this.auditor = auditor;
    }

    public String getAudiDate() {
        return audiDate;
    }

    public void setAudiDate(String audiDate) {
        this.audiDate = audiDate;
    }

    public String getPlanDate() {
        return planDate;
    }

    public void setPlanDate(String planDate) {
        this.planDate = planDate;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientTel() {
        return recipientTel;
    }

    public void setRecipientTel(String recipientTel) {
        this.recipientTel = recipientTel;
    }

    public List<HdDrugDTO> getDrugList() {
        return drugList;
    }

    public void setDrugList(List<HdDrugDTO> drugList) {
        this.drugList = drugList;
    }
}