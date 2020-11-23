package com.ngari.recipe.drug.model;

import ctd.schema.annotation.ItemProperty;

import java.io.Serializable;

/**
 * @author Zhou Wenfei
 * date 2020/11/18 19:30
 */
public class DrugPharmacyInventoryInfo implements Serializable {
    private static final long serialVersionUID = -6182775893717219897L;

    @ItemProperty(alias = "药房编码")
    private String pharmacyCode;

    @ItemProperty(alias = "药房名称")
    private String pharmacyName;

    @ItemProperty(alias = "药品库存")
    private String amount;

    public DrugPharmacyInventoryInfo() {

    }

    public DrugPharmacyInventoryInfo(String pharmacyCode, String pharmacyName, String amount) {
        this.pharmacyCode = pharmacyCode;
        this.pharmacyName = pharmacyName;
        this.amount = amount;
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

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }
}
