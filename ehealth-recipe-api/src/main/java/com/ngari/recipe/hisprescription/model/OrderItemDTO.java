package com.ngari.recipe.hisprescription.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
 * created by shiyuping on 2018/11/30
 */
@Schema
public class OrderItemDTO implements Serializable {
    private static final long serialVersionUID = 6730059180502358916L;
    @ItemProperty(alias="处方明细id")
    private String orderID;
    @ItemProperty(alias="药品代码（HIS药品代码）")
    private String drcode;
    @ItemProperty(alias="医保中心收费项目编码")
    private String medicalDrcode;
    @ItemProperty(alias="药品名称")
    private String drname;
    @ItemProperty(alias="药品规格")
    private String drmodel;
    @ItemProperty(alias="药品包装")
    private Integer pack;
    @ItemProperty(alias="药品包装单位")
    private String packUnit;
    @ItemProperty(alias="药品产地名称")
    private String drugManf;
    @ItemProperty(alias="药品产地编码")
    private String drugManfCode;
    @ItemProperty(alias="药品用法")
    private String admission;
    @ItemProperty(alias="药品用法名称")
    private String admissionName;
    @ItemProperty(alias="用品使用频度")
    private String frequency;
    @ItemProperty(alias="用品使用频度名称")
    private String frequencyName;

    /**为了兼容老数据从平台字典转换的admission、frequency字段
     * 特此新增了两种机构的频次用法代码，机构可以在运营平台维护后开处方选择了机构频次用法用该字段传入
     * */
    @ItemProperty(alias="机构的频次代码")
    private String organUsingRate;
    @ItemProperty(alias="机构的用法代码")
    private String organUsePathways;

    @ItemProperty(alias="医保频次编码")
    private String medicalFrequency;
    @ItemProperty(alias="每次剂量")
    private String dosage;
    @ItemProperty(alias="剂量单位")
    private String drunit;
   /* @ItemProperty(alias="药品日药量")
    private String dosageDay;*/
    @ItemProperty(alias="备注")
    private String remark;
    @ItemProperty(alias="用药天数")
    private String useDays;

    @ItemProperty(alias="总发药数量")
    private String totalDose;
    @ItemProperty(alias="药品单位")
    private String unit;
    @ItemProperty(alias="药品单价")
    private String price;

    @ItemProperty(alias="剂型代码")
    private String drugFormCode;
    @ItemProperty(alias="医保剂型代码")
    private String medicalDrugFormCode;
    @ItemProperty(alias="剂型名称")
    private String drugFormName;

    @ItemProperty(alias="医院药房编码")
    private String pharmacyCode;
    @ItemProperty(alias="医院药房名称")
    private String pharmacy;

    @ItemProperty(alias="药品序号")
    private Integer drugId;

    public Integer getDrugId() {
        return drugId;
    }

    public void setDrugId(Integer drugId) {
        this.drugId = drugId;
    }

    public OrderItemDTO(){}

    public String getTotalDose() {
        return totalDose;
    }

    public void setTotalDose(String totalDose) {
        this.totalDose = totalDose;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public String getDrcode() {
        return drcode;
    }

    public void setDrcode(String drcode) {
        this.drcode = drcode;
    }

    public String getDrname() {
        return drname;
    }

    public void setDrname(String drname) {
        this.drname = drname;
    }

    public String getDrmodel() {
        return drmodel;
    }

    public void setDrmodel(String drmodel) {
        this.drmodel = drmodel;
    }

    public Integer getPack() {
        return pack;
    }

    public void setPack(Integer pack) {
        this.pack = pack;
    }

    public String getPackUnit() {
        return packUnit;
    }

    public void setPackUnit(String packUnit) {
        this.packUnit = packUnit;
    }

    public String getDrugManf() {
        return drugManf;
    }

    public void setDrugManf(String drugManf) {
        this.drugManf = drugManf;
    }

    public String getAdmission() {
        return admission;
    }

    public void setAdmission(String admission) {
        this.admission = admission;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getDrunit() {
        return drunit;
    }

    public void setDrunit(String drunit) {
        this.drunit = drunit;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getUseDays() {
        return useDays;
    }

    public void setUseDays(String useDays) {
        this.useDays = useDays;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getDrugManfCode() {
        return drugManfCode;
    }

    public void setDrugManfCode(String drugManfCode) {
        this.drugManfCode = drugManfCode;
    }

    public String getMedicalDrcode() {
        return medicalDrcode;
    }

    public void setMedicalDrcode(String medicalDrcode) {
        this.medicalDrcode = medicalDrcode;
    }

    public String getAdmissionName() {
        return admissionName;
    }

    public void setAdmissionName(String admissionName) {
        this.admissionName = admissionName;
    }

    public String getFrequencyName() {
        return frequencyName;
    }

    public void setFrequencyName(String frequencyName) {
        this.frequencyName = frequencyName;
    }

    public String getDrugFormCode() {
        return drugFormCode;
    }

    public void setDrugFormCode(String drugFormCode) {
        this.drugFormCode = drugFormCode;
    }

    public String getDrugFormName() {
        return drugFormName;
    }

    public void setDrugFormName(String drugFormName) {
        this.drugFormName = drugFormName;
    }

    public String getMedicalFrequency() {
        return medicalFrequency;
    }

    public void setMedicalFrequency(String medicalFrequency) {
        this.medicalFrequency = medicalFrequency;
    }

    public String getMedicalDrugFormCode() {
        return medicalDrugFormCode;
    }

    public void setMedicalDrugFormCode(String medicalDrugFormCode) {
        this.medicalDrugFormCode = medicalDrugFormCode;
    }

    public String getOrganUsingRate() {
        return organUsingRate;
    }

    public void setOrganUsingRate(String organUsingRate) {
        this.organUsingRate = organUsingRate;
    }

    public String getOrganUsePathways() {
        return organUsePathways;
    }

    public void setOrganUsePathways(String organUsePathways) {
        this.organUsePathways = organUsePathways;
    }

    public String getPharmacyCode() {
        return pharmacyCode;
    }

    public void setPharmacyCode(String pharmacyCode) {
        this.pharmacyCode = pharmacyCode;
    }

    public String getPharmacy() {
        return pharmacy;
    }

    public void setPharmacy(String pharmacy) {
        this.pharmacy = pharmacy;
    }
}
