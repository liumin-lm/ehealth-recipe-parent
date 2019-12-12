package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Schema
@Table(name = "cdr_pharmacy")
@Access(AccessType.PROPERTY)
public class Pharmacy implements Serializable {

    private static final long serialVersionUID = 5081547294425425307L;

    @ItemProperty(alias = "药店ID")
    private Integer pharmacyId;

    @ItemProperty(alias = "关联的药企id")
    private Integer drugsenterpriseId;

    @ItemProperty(alias = "药店编码")
    private String pharmacyCode;

    @ItemProperty(alias = "药店名称")
    private String pharmacyName;

    @ItemProperty(alias = "药店地址")
    private String pharmacyAddress;

    @ItemProperty(alias = "药店经度")
    private String pharmacyLongitude;

    @ItemProperty(alias = "药店纬度")
    private String pharmacyLatitude;

    @ItemProperty(alias = "药店电话号")
    private String pharmacyPhone;

    @ItemProperty(alias = "启动状态")
    private Integer status;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "修改时间")
    private Date lastModify;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "pharmacyId", unique = true, nullable = false)
    public Integer getPharmacyId() {
        return pharmacyId;
    }

    public void setPharmacyId(Integer pharmacyId) {
        this.pharmacyId = pharmacyId;
    }

    public Integer getDrugsenterpriseId() {
        return drugsenterpriseId;
    }

    public void setDrugsenterpriseId(Integer drugsenterpriseId) {
        this.drugsenterpriseId = drugsenterpriseId;
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

    public String getPharmacyAddress() {
        return pharmacyAddress;
    }

    public void setPharmacyAddress(String pharmacyAddress) {
        this.pharmacyAddress = pharmacyAddress;
    }

    public String getPharmacyLongitude() {
        return pharmacyLongitude;
    }

    public void setPharmacyLongitude(String pharmacyLongitude) {
        this.pharmacyLongitude = pharmacyLongitude;
    }

    public String getPharmacyLatitude() {
        return pharmacyLatitude;
    }

    public void setPharmacyLatitude(String pharmacyLatitude) {
        this.pharmacyLatitude = pharmacyLatitude;
    }

    public String getPharmacyPhone() {
        return pharmacyPhone;
    }

    public void setPharmacyPhone(String pharmacyPhone) {
        this.pharmacyPhone = pharmacyPhone;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

}
