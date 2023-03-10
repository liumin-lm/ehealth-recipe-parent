package com.ngari.recipe.hisprescription.model;

import ctd.schema.annotation.ItemProperty;

import java.io.Serializable;
import java.math.BigDecimal;

public class RegulationRecipeDetailIndicatorsReq implements Serializable {

    private static final long serialVersionUID = -8399878782184986663L;

    @ItemProperty(alias = "处方明细Id")
    private Integer recipeDetailId;

    @ItemProperty(alias = "药品代码")
    private String  drcode;

    @ItemProperty(alias = "药品名称")
    private String  drname;

    @ItemProperty(alias = "药品规格")
    private String  drmodel;

    @ItemProperty(alias = "药物剂型")
    private String  dosageForm;

    @ItemProperty(alias = "药物剂型代码")
    private String  dosageFormCode;

    @ItemProperty(alias = "药品包装")
    private Integer pack;

    @ItemProperty(alias = "药品包装单位")
    private String  packUnit;

    @ItemProperty(alias = "药品产地名称")
    private String  drugManf;

    @ItemProperty(alias = "药品用法")
    private String  admission;

    @ItemProperty(alias = "药品用法名称")
    private String  admissionName;

    @ItemProperty(alias = "用品使用频度")
    private String  frequency;

    @ItemProperty(alias = "用品使用频度名称")
    private String  frequencyName;

    @ItemProperty(alias = "每次剂量")
    private String  dosage;

    @ItemProperty(alias = "剂量单位")
    private String  drunit;

    @ItemProperty(alias = "药物使用总剂量")
    private String  useDosage;

    @ItemProperty(alias = "药品日药量")
    private String dosageDay;

    @ItemProperty(alias = "药品药量")
    private String  dosageTotal;

    @ItemProperty(alias = "用药天数")
    private Integer useDays;

    @ItemProperty(alias = "药品单价")
    private BigDecimal price;

    @ItemProperty(alias = "药品总价")
    private BigDecimal totalPrice;

    @ItemProperty(alias = "中药饮片处方的详细描述")
    private String tcmDescribe;

    @ItemProperty(alias = "备注")
    private String  remark;

    @ItemProperty(alias = "机构的频次名称")
    private String organUsingRateText;

    @ItemProperty(alias = "his剂型名称")
    private String hisDrugForm;

    @ItemProperty(alias = "是否国家基本药品 0 否 1 是")
    private Integer nationalStandardDrugFlag;

    @ItemProperty(alias = "是否特殊使用级抗生素药物  0否  1是 ")
    private Integer specialUseAntibioticDrugFlag;


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

    public String getDosageTotal() {
        return dosageTotal;
    }

    public void setDosageTotal(String dosageTotal) {
        this.dosageTotal = dosageTotal;
    }

    public Integer getUseDays() {
        return useDays;
    }

    public void setUseDays(Integer useDays) {
        this.useDays = useDays;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getRecipeDetailId() {
        return recipeDetailId;
    }

    public void setRecipeDetailId(Integer recipeDetailId) {
        this.recipeDetailId = recipeDetailId;
    }

    public String getDosageForm() {
        return dosageForm;
    }

    public void setDosageForm(String dosageForm) {
        this.dosageForm = dosageForm;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getDosageDay() {
        return dosageDay;
    }

    public void setDosageDay(String dosageDay) {
        this.dosageDay = dosageDay;
    }

    public String getUseDosage() {
        return useDosage;
    }

    public void setUseDosage(String useDosage) {
        this.useDosage = useDosage;
    }

    public String getTcmDescribe() {
        return tcmDescribe;
    }

    public void setTcmDescribe(String tcmDescribe) {
        this.tcmDescribe = tcmDescribe;
    }

    public String getDosageFormCode() {
        return dosageFormCode;
    }

    public void setDosageFormCode(String dosageFormCode) {
        this.dosageFormCode = dosageFormCode;
    }

    public String getOrganUsingRateText() {
        return organUsingRateText;
    }

    public void setOrganUsingRateText(String organUsingRateText) {
        this.organUsingRateText = organUsingRateText;
    }

    public String getHisDrugForm() {
        return hisDrugForm;
    }

    public void setHisDrugForm(String hisDrugForm) {
        this.hisDrugForm = hisDrugForm;
    }

    public Integer getNationalStandardDrugFlag() {
        return nationalStandardDrugFlag;
    }

    public void setNationalStandardDrugFlag(Integer nationalStandardDrugFlag) {
        this.nationalStandardDrugFlag = nationalStandardDrugFlag;
    }

    public Integer getSpecialUseAntibioticDrugFlag() {
        return specialUseAntibioticDrugFlag;
    }

    public void setSpecialUseAntibioticDrugFlag(Integer specialUseAntibioticDrugFlag) {
        this.specialUseAntibioticDrugFlag = specialUseAntibioticDrugFlag;
    }
}
