package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 医生选择常用默认药房
 * @author liumin
 */
@Entity
@Schema
@Table(name = "doctor_common_pharmacy")
@Access(AccessType.PROPERTY)
@Deprecated
public  class DoctorCommonPharmacy implements java.io.Serializable{

    private static final long serialVersionUID = -7395577376998087750L;

    @ItemProperty(alias = "id")
    private  Integer id;

    @ItemProperty(alias = "organId")
    private  Integer organId;

    @ItemProperty(alias = "doctorId")
    private  Integer doctorId;

    @ItemProperty(alias = "西药药房ID")
    private  Integer wmPharmacyId;

    @ItemProperty(alias = "中成药药房ID")
    private  Integer pcmPharmacyId;

    @ItemProperty(alias = "中药药房ID'")
    private  Integer tcmPharmacyId;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "更新时间")
    private Date updateTime;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "organ_id")
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Column(name = "doctor_id")
    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    @Column(name = "wm_pharmacy_id")
    public Integer getWmPharmacyId() {
        return wmPharmacyId;
    }

    public void setWmPharmacyId(Integer wmPharmacyId) {
        this.wmPharmacyId = wmPharmacyId;
    }

    @Column(name = "pcm_pharmacy_id")
    public Integer getPcmPharmacyId() {
        return pcmPharmacyId;
    }

    public void setPcmPharmacyId(Integer pcmPharmacyId) {
        this.pcmPharmacyId = pcmPharmacyId;
    }

    @Column(name = "tcm_pharmacy_id")
    public Integer getTcmPharmacyId() {
        return tcmPharmacyId;
    }

    public void setTcmPharmacyId(Integer tcmPharmacyId) {
        this.tcmPharmacyId = tcmPharmacyId;
    }

    @Column(name = "create_time", length = 19)
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Column(name = "update_time", length = 19)
    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }


}
