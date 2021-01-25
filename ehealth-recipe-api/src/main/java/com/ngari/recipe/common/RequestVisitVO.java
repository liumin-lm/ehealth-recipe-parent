package com.ngari.recipe.common;

import java.io.Serializable;

public class RequestVisitVO implements Serializable {

    private Integer organId;

    private Integer clinicId;

    private Integer doctor;

    private String mpiid;

    private Integer status;

    public Integer getClinicId() {
        return clinicId;
    }

    public void setClinicId(Integer clinicId) {
        this.clinicId = clinicId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
