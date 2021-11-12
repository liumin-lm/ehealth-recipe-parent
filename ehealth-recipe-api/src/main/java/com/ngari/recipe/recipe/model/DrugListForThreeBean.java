package com.ngari.recipe.recipe.model;

import java.io.Serializable;

public class DrugListForThreeBean implements Serializable{
    private static final long serialVersionUID = 7134500240084312838L;

    private String drugCode;
    private String drugName;
    private String specification;
    private String licenseNumber;
    private String standardCode;
    private String producer;
    private String total;
    private String drugUnit;
    private String pack;
    private String useDose;
    private String drugFee;
    private String medicalFee;
    private String uesDays;
    private String pharmNo;
    private String usingRate;
    private String usePathways;
    private String usingRateText;
    private String usePathwaysText;
    private String drugTotalFee;
    private String memo;
    private String drugForm;

    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getStandardCode() {
        return standardCode;
    }

    public void setStandardCode(String standardCode) {
        this.standardCode = standardCode;
    }

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public String getDrugUnit() {
        return drugUnit;
    }

    public void setDrugUnit(String drugUnit) {
        this.drugUnit = drugUnit;
    }

    public String getPack() {
        return pack;
    }

    public void setPack(String pack) {
        this.pack = pack;
    }

    public String getUseDose() {
        return useDose;
    }

    public void setUseDose(String useDose) {
        this.useDose = useDose;
    }

    public String getDrugFee() {
        return drugFee;
    }

    public void setDrugFee(String drugFee) {
        this.drugFee = drugFee;
    }

    public String getMedicalFee() {
        return medicalFee;
    }

    public void setMedicalFee(String medicalFee) {
        this.medicalFee = medicalFee;
    }

    public String getDrugTotalFee() {
        return drugTotalFee;
    }

    public void setDrugTotalFee(String drugTotalFee) {
        this.drugTotalFee = drugTotalFee;
    }

    public String getUesDays() {
        return uesDays;
    }

    public void setUesDays(String uesDays) {
        this.uesDays = uesDays;
    }

    public String getPharmNo() {
        return pharmNo;
    }

    public void setPharmNo(String pharmNo) {
        this.pharmNo = pharmNo;
    }

    public String getUsingRate() {
        return usingRate;
    }

    public void setUsingRate(String usingRate) {
        this.usingRate = usingRate;
    }

    public String getUsePathways() {
        return usePathways;
    }

    public void setUsePathways(String usePathways) {
        this.usePathways = usePathways;
    }

    public String getUsingRateText() {
        return usingRateText;
    }

    public void setUsingRateText(String usingRateText) {
        this.usingRateText = usingRateText;
    }

    public String getUsePathwaysText() {
        return usePathwaysText;
    }

    public void setUsePathwaysText(String usePathwaysText) {
        this.usePathwaysText = usePathwaysText;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getDrugForm() {
        return drugForm;
    }

    public void setDrugForm(String drugForm) {
        this.drugForm = drugForm;
    }
}
