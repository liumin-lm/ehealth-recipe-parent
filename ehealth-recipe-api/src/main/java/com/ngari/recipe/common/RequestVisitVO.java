package com.ngari.recipe.common;

import ctd.schema.annotation.ItemProperty;

import java.io.Serializable;

public class RequestVisitVO implements Serializable {

    @ItemProperty(alias = "机构Id")
    private Integer organId;

    @ItemProperty(alias = "复诊Id")
    private Integer clinicId;

    @ItemProperty(alias = "医生Id")
    private Integer doctor;

    @ItemProperty(alias = "患者Id")
    private String mpiid;

    public RequestVisitVO() {
    }

    public RequestVisitVO(Integer organId, Integer clinicId, Integer doctor, String mpiid) {
        this.organId = organId;
        this.clinicId = clinicId;
        this.doctor = doctor;
        this.mpiid = mpiid;
    }

    public Integer getClinicId() {
        return clinicId;
    }

    public void setClinicId(Integer clinicId) {
        this.clinicId = clinicId;
    }

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public Integer getDoctor() {
        return doctor;
    }

    public void setDoctor(Integer doctor) {
        this.doctor = doctor;
    }

    public String getMpiid() {
        return mpiid;
    }

    public void setMpiid(String mpiid) {
        this.mpiid = mpiid;
    }


}
