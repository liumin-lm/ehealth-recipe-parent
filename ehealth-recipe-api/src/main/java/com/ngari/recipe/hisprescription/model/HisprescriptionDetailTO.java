package com.ngari.recipe.hisprescription.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author： 0184/yu_yun
 * @date： 2018/6/28
 * @description： 医院处方记录详情
 * @version： 1.0
 */
@Schema
public class HisprescriptionDetailTO implements Serializable{

    private static final long serialVersionUID = 8995040420146491781L;

    @ItemProperty(alias="自增ID")
    private Integer recipedetailId;

    @ItemProperty(alias="处方ID")
    private Integer recipeId;

    @ItemProperty(alias="HIS医嘱号")
    private String orderNo;

    @ItemProperty(alias="医院药品代码")
    private String drcode;

    @ItemProperty(alias="药品名称")
    private String drname;

    @ItemProperty(alias="药品规格")
    private String drmodel;

    @ItemProperty(alias="药品包装数量")
    private Integer pack;

    @ItemProperty(alias="药品包装单位")
    private String packUnit;

    @ItemProperty(alias="药品产地代码")
    private String manfcode;

    @ItemProperty(alias="药品用法")
    private String admission;

    @ItemProperty(alias="用品使用频度")
    private String frequency;

    @ItemProperty(alias="每次剂量")
    private String dosage;

    @ItemProperty(alias="剂量单位")
    private String drunit;

    @ItemProperty(alias="药品日药量")
    private Integer dosageDay;

    @ItemProperty(alias="单个药品数量")
    private String number;

    @ItemProperty(alias="单个药品金额")
    private BigDecimal amount;

    @ItemProperty(alias="单个药品总金额")
    private BigDecimal totalAmount;

    public Integer getRecipedetailId() {
        return recipedetailId;
    }

    public void setRecipedetailId(Integer recipedetailId) {
        this.recipedetailId = recipedetailId;
    }

    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
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

    public String getManfcode() {
        return manfcode;
    }

    public void setManfcode(String manfcode) {
        this.manfcode = manfcode;
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

    public Integer getDosageDay() {
        return dosageDay;
    }

    public void setDosageDay(Integer dosageDay) {
        this.dosageDay = dosageDay;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
