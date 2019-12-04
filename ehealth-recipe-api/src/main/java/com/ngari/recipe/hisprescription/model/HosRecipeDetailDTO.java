package com.ngari.recipe.hisprescription.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * created by shiyuping on 2019/11/11
 * @author shiyuping
 */
public class HosRecipeDetailDTO implements Serializable {

    private static final long serialVersionUID = -2452444769346095979L;
    private String recipeDeatilCode;
    private String drugCode;
    private String drugName;
    private BigDecimal price;
    //药品发药数量
    private Double amount;
    //药品规格
    private String drModel;
    //频次名称 pcmc
    private String usingRate;
    //用法名称 yfmc
    private String usePathWays;
    //剂量单位 jldw
    private String useDoseUnit;
    //药品剂量 ypjl
    private String useDose;
    //药品单位 ypdw
    private String unit;
    //用药天数 yyts
    private String days;

    public String getRecipeDeatilCode() {
        return recipeDeatilCode;
    }

    public void setRecipeDeatilCode(String recipeDeatilCode) {
        this.recipeDeatilCode = recipeDeatilCode;
    }

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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getDrModel() {
        return drModel;
    }

    public void setDrModel(String drModel) {
        this.drModel = drModel;
    }

    public String getUsingRate() {
        return usingRate;
    }

    public void setUsingRate(String usingRate) {
        this.usingRate = usingRate;
    }

    public String getUsePathWays() {
        return usePathWays;
    }

    public void setUsePathWays(String usePathWays) {
        this.usePathWays = usePathWays;
    }

    public String getUseDoseUnit() {
        return useDoseUnit;
    }

    public void setUseDoseUnit(String useDoseUnit) {
        this.useDoseUnit = useDoseUnit;
    }

    public String getUseDose() {
        return useDose;
    }

    public void setUseDose(String useDose) {
        this.useDose = useDose;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDays() {
        return days;
    }

    public void setDays(String days) {
        this.days = days;
    }
}
