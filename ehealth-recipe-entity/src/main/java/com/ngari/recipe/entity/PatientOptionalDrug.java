package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import org.hibernate.annotations.DynamicInsert;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @description： 患者自选药品
 * @author： whf
 * @date： 2021-11-22 17:04
 */
@Entity
@Schema
@Table(name = "patient_optional_drug")
@Access(AccessType.PROPERTY)
@DynamicInsert
public class PatientOptionalDrug implements Serializable {

    @ItemProperty(alias = "自增id")
    private Integer id;

    @ItemProperty(alias = "药品序号")
    private Integer drugId;

    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "就诊序号(对应来源的业务id)")
    private Integer clinicId;

    @ItemProperty(alias = "机构药品编号")
    private String organDrugCode;

    @ItemProperty(alias = "患者指定的药品数量")
    private Integer patientDrugNum;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "修改时间")
    private Date modifiedTime;

    @Column(name = "patient_drug_num")
    public Integer getPatientDrugNum() {
        return patientDrugNum;
    }

    public void setPatientDrugNum(Integer patientDrugNum) {
        this.patientDrugNum = patientDrugNum;
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "drug_id")
    public Integer getDrugId() {
        return drugId;
    }

    public void setDrugId(Integer drugId) {
        this.drugId = drugId;
    }

    @Column(name = "organ_id")
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Column(name = "clinic_id")
    public Integer getClinicId() {
        return clinicId;
    }

    public void setClinicId(Integer clinicId) {
        this.clinicId = clinicId;
    }

    @Column(name = "organ_drug_code")
    public String getOrganDrugCode() {
        return organDrugCode;
    }

    public void setOrganDrugCode(String organDrugCode) {
        this.organDrugCode = organDrugCode;
    }


    @Column(name = "create_time")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Column(name = "modified_time")
    public Date getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(Date modifiedTime) {
        this.modifiedTime = modifiedTime;
    }
}
