package com.ngari.recipe.hisprescription.model;

import com.ngari.recipe.common.anno.Verify;
import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * date:2017/5/12.
 */
@Schema
public class HospitalDrugDTO implements Serializable {

    private static final long serialVersionUID = -634493416078418209L;

    @Verify(desc = "药品编码")
    private String drugCode;

    @Verify(desc = "药品商品名称")
    private String drugName;

    @Verify(desc = "药品规格")
    private String specification;

    @Verify(isNotNull = false, desc = "批准文号")
    private String licenseNumber;

    @Verify(isNotNull = false, desc = "药品本位码")
    private String standardCode;

    @Verify(desc = "药品生产厂家")
    private String producer;

    @Verify(desc = "开药总数")
    private String total;

    @Verify(desc = "每次使用剂量")
    private String useDose;

    private String useDoseUnit;

    @Verify(desc = "药品单价", isMoney = true)
    private String drugFee;

    @Verify(isNotNull = false, desc = "医保报销金额", isMoney = true)
    private String medicalFee;

    @Verify(desc = "药品总价", isMoney = true)
    private String drugTotalFee;

    @Verify(desc = "用药天数", isInt = true)
    private String uesDays;

    @Verify(isNotNull = false, desc = "取药窗口")
    private String pharmNo;

    @Verify(desc = "用药频率")
    private String usingRate;

    @Verify(desc = "用药途径")
    private String usePathways;

    @Verify(isNotNull = false, desc = "药品使用备注", maxLength = 50)
    private String memo;

    @Verify(isNotNull = false, desc = "处方明细编码")
    private String recipedtlno;

    @Verify(isNotNull = false, desc = "药物单位")
    private String drugUnit;

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

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getUseDoseUnit() {
        return useDoseUnit;
    }

    public void setUseDoseUnit(String useDoseUnit) {
        this.useDoseUnit = useDoseUnit;
    }

    public String getRecipedtlno() {
        return recipedtlno;
    }

    public void setRecipedtlno(String recipedtlno) {
        this.recipedtlno = recipedtlno;
    }

    public String getDrugUnit() {
        return drugUnit;
    }

    public void setDrugUnit(String drugUnit) {
        this.drugUnit = drugUnit;
    }
}
