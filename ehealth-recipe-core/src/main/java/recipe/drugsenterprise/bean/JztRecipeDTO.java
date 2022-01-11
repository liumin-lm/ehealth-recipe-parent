package recipe.drugsenterprise.bean;

import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;
import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * @author yinsheng
 * @date 2019\3\14 0014 15:33
 */
@Schema
public class JztRecipeDTO implements Serializable {
    private static final long serialVersionUID = 2278967962954405876L;

    private String certificateType;

    @Desensitizations(type = DesensitizationsType.IDCARD)
    private String certificate;

    private String patientName;

    @Desensitizations(type = DesensitizationsType.MOBILE)
    private String patientTel;

    private String patientNumber;

    @Desensitizations(type = DesensitizationsType.ADDRESS)
    private String patientAddress;

    private String recipeCode;

    private String clinicOrgan;

    private String recipeType;

    private String organId;

    private String organName;

    private String departId;

    private String departName;

    private String doctorNumber;

    private String doctorName;

    private String createDate;

    private String recipeFee;

    private String actualFee;

    private String couponFee;

    private String decoctionFee;

    private String medicalFee;

    private String expressFee;

    private String orderTotalFee;

    private String organDiseaseName;

    private String organDiseaseId;

    private String memo;

//    private String payMode;

    private String payFlag;

    private String giveMode;

    private String giveUser;

    private String status;

    private String medicalPayFlag;

    private String distributionFlag;

    private String distributorCode;

    private String distributorName;

    private String pharmacyCode;

    private String pharmacyName;

    private String recipeMemo;

    private String tcmUsePathways;

    private String tcmUsingRate;

    private String tcmNum;

    private List<JztDrugDTO> drugList;

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

    public String getPatientNumber() {
        return patientNumber;
    }

    public void setPatientNumber(String patientNumber) {
        this.patientNumber = patientNumber;
    }

    public String getPatientAddress() {
        return patientAddress;
    }

    public void setPatientAddress(String patientAddress) {
        this.patientAddress = patientAddress;
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

    public String getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(String recipeType) {
        this.recipeType = recipeType;
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
//
//    public String getPayMode() {
//        return payMode;
//    }
//
//    public void setPayMode(String payMode) {
//        this.payMode = payMode;
//    }

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

    public List<JztDrugDTO> getDrugList() {
        return drugList;
    }

    public void setDrugList(List<JztDrugDTO> drugList) {
        this.drugList = drugList;
    }
}
