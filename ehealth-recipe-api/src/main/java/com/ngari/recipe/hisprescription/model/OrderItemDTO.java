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
    @ItemProperty(alias="平台药品ID")
    private String orderID;
    @ItemProperty(alias="药品代码（HIS药品代码）")
    private String drcode;
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
    @ItemProperty(alias="药品用法")
    private String admission;
    @ItemProperty(alias="用品使用频度")
    private String frequency;
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
}
