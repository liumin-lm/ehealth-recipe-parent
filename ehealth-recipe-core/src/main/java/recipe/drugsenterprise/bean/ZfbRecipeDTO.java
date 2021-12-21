package recipe.drugsenterprise.bean;

import com.ngari.recipe.common.anno.Verify;
import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * zfb处方对象
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * date:2017/4/17.
 * 对接药企 无需脱敏
 */
@Schema
public class ZfbRecipeDTO implements Serializable {

    private static final long serialVersionUID = -1547090960201298723L;

    @Verify(isNotNull = false, desc = "就诊序号")
    private String clinicId;

    @Verify(isNotNull = false, desc = "患者id")
    private String mpiId;

    @Verify(desc = "患者证件类型")
    private String certificateType;

    @Verify(desc = "患者证件号")
    private String certificate;

    @Verify(desc = "患者姓名")
    private String patientName;

    @Verify(desc = "患者电话")
    private String patientTel;

    @Verify(isNotNull = false, desc = "患者地址", maxLength = 100)
    private String patientAddress;

    @Verify(isNotNull = false, desc = "患者医院病历号")
    private String patientNumber;

    @Verify(desc = "性别", isInt = true)
    private String patientSex;

    @Verify(desc = "处方号")
    private String recipeCode;

    @Verify(isNotNull = false, desc = "开方机构序号", isInt = true)
    private String clinicOrgan;

    @Verify(desc = "组织机构编码", maxLength = 30)
    private String organId;

    @Verify(desc = "组织机构名称", maxLength = 30)
    private String organName;

    @Verify(desc = "处方类型", isInt = true)
    private String recipeType;

    @Verify(desc = "开方科室", isInt = true)
    private String departId;

    @Verify(desc = "开方科室名称")
    private String departName;

    @Verify(desc = "开方医生工号")
    private String doctorNumber;

    @Verify(desc = "开方医生姓名")
    private String doctorName;

    @Verify(desc = "开方时间", isDate = true)
    private String createDate;

    @Verify(desc = "处方金额", isMoney = true)
    private String recipeFee;

    @Verify(desc = "实际支付金额", isMoney = true)
    private String actualFee;

    @Verify(isNotNull = false, desc = "优惠金额", isMoney = true)
    private String couponFee;

    @Verify(isNotNull = false, desc = "待煎费或者膏方制作费", isMoney = true)
    private String decoctionFee;

    @Verify(isNotNull = false, desc = "医保报销金额", isMoney = true)
    private String medicalFee;

    @Verify(isNotNull = false, desc = "配送费", isMoney = true)
    private String expressFee;

    @Verify(desc = "订单总价，不计算减免的", isMoney = true)
    private String orderTotalFee;

    @Verify(desc = "诊断疾病名称", maxLength = 250)
    private String organDiseaseName;

    @Verify(desc = "诊断疾病编码", maxLength = 100)
    private String organDiseaseId;

    @Verify(isNotNull = false, desc = "诊断备注", maxLength = 160)
    private String memo;

    @Verify(isNotNull = false, desc = "审核机构")
    private String checkOrgan;

    @Verify(isNotNull = false, desc = "审核时间", isDate = true)
    private String checkDate;

    @Verify(isNotNull = false, desc = "审核医生姓名")
    private String checkerName;

    @Verify(isNotNull = false, desc = "审核医生工号")
    private String checkerNumber;

    @Verify(isNotNull = false, desc = "审核医生电话")
    private String checkerTel;

    @Verify(isNotNull = false, desc = "处方审核备注", maxLength = 255)
    private String checkFailMemo;

    @Verify(isNotNull = false, desc = "药师审核不通过，医生补充说明", maxLength = 100)
    private String supplementaryMemo;

    @Verify(isNotNull = false, desc = "支付方式", isInt = true)
    private String payMode;

    @Verify(isNotNull = false, desc = "发药方式", isInt = true)
    private String giveMode;

    @Verify(isNotNull = false, desc = "发药人姓名")
    private String giveUser;

    @Verify(desc = "处方状态", isInt = true)
    private String status;

    @Verify(desc = "是否医保支付", isInt = true)
    private String medicalPayFlag;

    @Verify(desc = "是否只走配送", isInt = true)
    private String distributionFlag;

    @Verify(isNotNull = false, desc = "是否已支付", isInt = true)
    private String payFlag;

    @Verify(isNotNull = false, desc = "处方备注", maxLength = 100)
    private String recipeMemo;

    @Verify(isNotNull = false, desc = "中药处方用法")
    private String tcmUsePathways;

    @Verify(isNotNull = false, desc = "中药处方用量")
    private String tcmUsingRate;

    @Verify(isNotNull = false, desc = "帖数", isInt = true)
    private String tcmNum;

    @Verify(isNotNull = false, desc = "配送厂商编码")
    private String distributorCode;

    @Verify(desc = "配送厂商名称")
    private String distributorName;

    @Verify(isNotNull = false, desc = "药店编码")
    private String pharmacyCode;

    @Verify(desc = "药店名称")
    private String pharmacyName;

    @Verify(desc = "药品详情")
    private List<ZfbDrugDTO> drugList;

    public String getClinicId() {
        return clinicId;
    }

    public void setClinicId(String clinicId) {
        this.clinicId = clinicId;
    }

    public String getMpiId() {
        return mpiId;
    }

    public void setMpiId(String mpiId) {
        this.mpiId = mpiId;
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

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientTel() {
        return patientTel;
    }

    public void setPatientTel(String patientTel) {
        this.patientTel = patientTel;
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

    public String getPatientSex() {
        return patientSex;
    }

    public void setPatientSex(String patientSex) {
        this.patientSex = patientSex;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public String getClinicOrgan() {
        return clinicOrgan;
    }

    public void setClinicOrgan(String clinicOrgan) {
        this.clinicOrgan = clinicOrgan;
    }

    public String getOrganId() {
        return organId;
    }

    public void setOrganId(String organId) {
        this.organId = organId;
    }

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
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

    public String getCheckOrgan() {
        return checkOrgan;
    }

    public void setCheckOrgan(String checkOrgan) {
        this.checkOrgan = checkOrgan;
    }

    public String getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(String checkDate) {
        this.checkDate = checkDate;
    }

    public String getCheckerName() {
        return checkerName;
    }

    public void setCheckerName(String checkerName) {
        this.checkerName = checkerName;
    }

    public String getCheckerNumber() {
        return checkerNumber;
    }

    public void setCheckerNumber(String checkerNumber) {
        this.checkerNumber = checkerNumber;
    }

    public String getCheckerTel() {
        return checkerTel;
    }

    public void setCheckerTel(String checkerTel) {
        this.checkerTel = checkerTel;
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

    public String getPayMode() {
        return payMode;
    }

    public void setPayMode(String payMode) {
        this.payMode = payMode;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMedicalPayFlag() {
        return medicalPayFlag;
    }

    public void setMedicalPayFlag(String medicalPayFlag) {
        this.medicalPayFlag = medicalPayFlag;
    }

    public String getDistributionFlag() {
        return distributionFlag;
    }

    public void setDistributionFlag(String distributionFlag) {
        this.distributionFlag = distributionFlag;
    }

    public String getPayFlag() {
        return payFlag;
    }

    public void setPayFlag(String payFlag) {
        this.payFlag = payFlag;
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

    public String getPharmacyCode() {
        return pharmacyCode;
    }

    public void setPharmacyCode(String pharmacyCode) {
        this.pharmacyCode = pharmacyCode;
    }

    public String getPharmacyName() {
        return pharmacyName;
    }

    public void setPharmacyName(String pharmacyName) {
        this.pharmacyName = pharmacyName;
    }

    public List<ZfbDrugDTO> getDrugList() {
        return drugList;
    }

    public void setDrugList(List<ZfbDrugDTO> drugList) {
        this.drugList = drugList;
    }
}




