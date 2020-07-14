package com.ngari.recipe.recipe.model;

import java.io.Serializable;
import java.util.List;

public class RecipeAndOrderDetailBean implements Serializable{
    private static final long serialVersionUID = -1543466696121633673L;

    private String msg;
    private String certificateType;
    private String certificate;
    private String patientName;
    private String patientTel;
    private String patientAddress;
    private String patientNumber;
    private String recipeCode;
    private String recipeId;
    private String clinicOrgan;
    private String organId;
    private String organName;
    private String recipeType;
    private String departId;
    private String departName;
    private String doctorNumber;
    private String doctorName;
    private String createDate;
    private String recipeFee;
    private String actualFee;
    private String couponFee;
    private String decoctionFee;
    private String tcmFee;
    private String auditFee;
    private String registerFee;
    private String medicalFee;
    private String province;
    private String city;
    private String district;
    private String street;
    private String expressFee;
    private String receiver;
    private String recMobile;
    private String recAddress;
    private String orderTotalFee;
    private String outTradeNo;
    private String tradeNo;
    private String organDiseaseName;
    private String organDiseaseId;
    private String memo;
    private String payMode;
    private String payFlag;
    private String giveMode;
    private String status;
    private String medicalPayFlag;
    private String distributionFlag;
    private String recipeMemo;
    private String tcmUsePathways;
    private String tcmUsingRate;
    private String tcmNum;
    private String pharmacyCode;
    private String pharmacyName;
    private String recipeSignImg;
    private String recipeSignImgUrl;
    private String decoctionFlag;
    private List<DrugListForThreeBean> drugList;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
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

    public String getAuditFee() {
        return auditFee;
    }

    public void setAuditFee(String auditFee) {
        this.auditFee = auditFee;
    }

    public String getRegisterFee() {
        return registerFee;
    }

    public void setRegisterFee(String registerFee) {
        this.registerFee = registerFee;
    }

    public String getMedicalFee() {
        return medicalFee;
    }

    public void setMedicalFee(String medicalFee) {
        this.medicalFee = medicalFee;
    }

    public String getTcmFee() {
        return tcmFee;
    }

    public void setTcmFee(String tcmFee) {
        this.tcmFee = tcmFee;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getExpressFee() {
        return expressFee;
    }

    public void setExpressFee(String expressFee) {
        this.expressFee = expressFee;
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

    public String getRecAddress() {
        return recAddress;
    }

    public void setRecAddress(String recAddress) {
        this.recAddress = recAddress;
    }

    public String getOrderTotalFee() {
        return orderTotalFee;
    }

    public void setOrderTotalFee(String orderTotalFee) {
        this.orderTotalFee = orderTotalFee;
    }

    public String getOutTradeNo() {
        return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo) {
        this.outTradeNo = outTradeNo;
    }

    public String getTradeNo() {
        return tradeNo;
    }

    public void setTradeNo(String tradeNo) {
        this.tradeNo = tradeNo;
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

    public String getRecipeSignImg() {
        return recipeSignImg;
    }

    public void setRecipeSignImg(String recipeSignImg) {
        this.recipeSignImg = recipeSignImg;
    }

    public String getRecipeSignImgUrl() {
        return recipeSignImgUrl;
    }

    public void setRecipeSignImgUrl(String recipeSignImgUrl) {
        this.recipeSignImgUrl = recipeSignImgUrl;
    }

    public String getDecoctionFlag() {
        return decoctionFlag;
    }

    public void setDecoctionFlag(String decoctionFlag) {
        this.decoctionFlag = decoctionFlag;
    }

    public List<DrugListForThreeBean> getDrugList() {
        return drugList;
    }

    public void setDrugList(List<DrugListForThreeBean> drugList) {
        this.drugList = drugList;
    }
}
