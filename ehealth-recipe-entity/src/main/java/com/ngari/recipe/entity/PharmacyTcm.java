package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 药房
 * @author renfuhao
 */
@Entity
@Schema
@Table(name = "recipe_pharmacy")
@Access(AccessType.PROPERTY)
public  class PharmacyTcm implements java.io.Serializable{

    private static final long serialVersionUID = -7395577376998087750L;

    @ItemProperty(alias = "药房ID")
    private  Integer pharmacyId;

    @ItemProperty(alias = "药房编码")
    private String pharmacyCode;

    @ItemProperty(alias = "药房名称")
    private String pharmacyName;

    @ItemProperty(alias = "药房分类")
    private String category;

    @ItemProperty(alias = "是否默认")
    private Boolean whDefault;

    @ItemProperty(alias = "药房排序")
    private Integer sort;

    @ItemProperty(alias = "机构ID")
    private Integer organId;

    @ItemProperty(alias = "药房类型")
    private String type;

    @ItemProperty(alias = "药房类型")
    private String pharmacyCategray;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "pharmacyId", unique = true, nullable = false)
    public Integer getPharmacyId() {
        return pharmacyId;
    }

    public void setPharmacyId(Integer pharmacyId) {
        this.pharmacyId = pharmacyId;
    }

    @Column(name = "pharmacyCode")
    public String getPharmacyCode() {
        return pharmacyCode;
    }

    public void setPharmacyCode(String pharmacyCode) {
        this.pharmacyCode = pharmacyCode;
    }

    @Column(name = "pharmacyName")
    public String getPharmacyName() {
        return pharmacyName;
    }

    public void setPharmacyName(String pharmacyName) {
        this.pharmacyName = pharmacyName;
    }

    @Column(name = "category")
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Column(name = "whDefault")
    public Boolean getWhDefault() {
        return whDefault;
    }

    public void setWhDefault(Boolean whDefault) {
        this.whDefault = whDefault;
    }


    @Column(name = "sort")
    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    @Column(name = "organId")
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Column(name = "type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Column(name = "pharmacyCategray")
    public String getPharmacyCategray() {
        return pharmacyCategray;
    }

    public void setPharmacyCategray(String pharmacyCategray) {
        this.pharmacyCategray = pharmacyCategray;
    }
}
