package com.ngari.recipe.hisprescription.model;

import java.io.Serializable;
import java.util.List;

/**
 * 第三方医院处方对象
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * date:2017/4/17.
 */
public class HospitalRecipeDTO implements Serializable {

    private static final long serialVersionUID = 5376913267069813730L;

    private String clinicId;

    private String mpiId;

    private String certificateType;

    private String certificate;

    private String patientName;

    private String patientTel;

    private String patientNumber;

    private String recipeCode;

    private String clinicOrgan;

    private String recipeType;

    private String departId;

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

    private String checkOrgan;

    private String checkDate;

    private String checkerName;

    private String checkerNumber;

    private String checkerTel;

    private String checkFailMemo;

    private String supplementaryMemo;

    private String payMode;

    private String giveMode;

    private String giveUser;

    private String status;

    private String medicalPayFlag;

    private String distributionFlag;

    private String payFlag;

    private String recipeMemo;

    private String tcmUsePathways;

    private String tcmUsingRate;

    private String tcmNum;

    private List<HospitalDrugDTO> drugList;

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

    public String getDepartId() {
        return departId;
    }

    public void setDepartId(String departId) {
        this.departId = departId;
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

    public List<HospitalDrugDTO> getDrugList() {
        return drugList;
    }

    public void setDrugList(List<HospitalDrugDTO> drugList) {
        this.drugList = drugList;
    }
}




