package com.ngari.recipe.hisprescription.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class RegulationRecipeDetailIndicatorsReq implements Serializable {


    private static final long serialVersionUID = -8399878782184986663L;
    private Integer recipeDetailId;//处方明细Id
    private String  drcode;//	药品代码
    private String  drname;//	药品名称
    private String  drmodel;//	药品规格
    private String  dosageForm;//药物剂型
    private String  dosageFormCode;// 药物剂型代码
    private Integer pack;//	药品包装
    private String  packUnit;//	药品包装单位
    private String  drugManf;//	药品产地名称
    private String  admission;//	药品用法
    private String  admissionName;//	药品用法名称
    private String  frequency;//	用品使用频度
    private String  frequencyName;//	用品使用频度名称
    private String  dosage;//	每次剂量
    private String  drunit;//	剂量单位
    private String  useDosage;//药物使用总剂量
    private String dosageDay; // 药品日药量
    private String  dosageTotal;//	药品药量
    private Integer useDays;//	用药天数
    private BigDecimal price;//药品单价
    private BigDecimal totalPrice;//药品总价
    private String tcmDescribe;//中药饮片处方的详细描述
    private String  remark;//	备注

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
}
