package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/5/27.
 */
@Schema
public class RecipeRollingInfoBean implements Serializable {

    private static final long serialVersionUID = -3694270598793030159L;

    @ItemProperty(alias = "开方科室")
    @Dictionary(id = "eh.base.dictionary.Depart")
    private Integer depart;

    @ItemProperty(alias = "开方医生")
    @Dictionary(id = "eh.base.dictionary.Doctor")
    private Integer doctor;

    @ItemProperty(alias = "开方机构")
    @Dictionary(id = "eh.base.dictionary.Organ")
    private Integer clinicOrgan;

    private String mpiId;

    private String patientName;

    public RecipeRollingInfoBean() {
    }

    public RecipeRollingInfoBean(Integer clinicOrgan, Integer depart, Integer doctor, String mpiId) {
        this.clinicOrgan = clinicOrgan;
        this.depart = depart;
        this.doctor = doctor;
        this.mpiId = mpiId;
    }

    public Integer getDepart() {
        return depart;
    }

    public void setDepart(Integer depart) {
        this.depart = depart;
    }

    public Integer getDoctor() {
        return doctor;
    }

    public void setDoctor(Integer doctor) {
        this.doctor = doctor;
    }

    public Integer getClinicOrgan() {
        return clinicOrgan;
    }

    public void setClinicOrgan(Integer clinicOrgan) {
        this.clinicOrgan = clinicOrgan;
    }

    public String getMpiId() {
        return mpiId;
    }

    public void setMpiId(String mpiId) {
        this.mpiId = mpiId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }
}
