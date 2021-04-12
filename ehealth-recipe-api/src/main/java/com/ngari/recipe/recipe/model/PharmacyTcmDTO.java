package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
 * @author rfh
 */
@Schema
public class PharmacyTcmDTO implements Serializable {

    private static final long serialVersionUID = 4317215992191031409L;

    private  Integer pharmacyId;

    private String pharmacyCode;

    private String pharmacyName;

    private String category;

    private Boolean whDefault;

    private Integer sort;

    private Integer organId;

    private String type;

    private String pharmacyCategray;

    public Integer getPharmacyId() {
        return pharmacyId;
    }

    public void setPharmacyId(Integer pharmacyId) {
        this.pharmacyId = pharmacyId;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getWhDefault() {
        return whDefault;
    }

    public void setWhDefault(Boolean whDefault) {
        this.whDefault = whDefault;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPharmacyCategray() {
        return pharmacyCategray;
    }

    public void setPharmacyCategray(String pharmacyCategray) {
        this.pharmacyCategray = pharmacyCategray;
    }
}
