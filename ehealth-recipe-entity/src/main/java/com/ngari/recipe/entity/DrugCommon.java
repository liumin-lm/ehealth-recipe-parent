package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 常用药表
 * @author fuzi
 */
@Schema
@Entity
@Table(name = "cdr_drug_common")
@Access(AccessType.PROPERTY)
@DynamicInsert
@DynamicUpdate
public class DrugCommon {
    @ItemProperty(alias = "机构代码")
    private Integer id;
    @ItemProperty(alias = "机构代码")
    private Integer organId;
    @ItemProperty(alias = "医生身份ID")
    private Integer doctorId;
    @ItemProperty(alias = "机构药品唯一索引")
    private String organDrugCode;
    @ItemProperty(alias = "药品序号")
    private Integer drugId;
    @ItemProperty(alias = "药品类型")
    private Integer drugType;
    @ItemProperty(alias = "排序字段")
    private Integer sort;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
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

    @Column(name = "organ_drug_code")
    public String getOrganDrugCode() {
        return organDrugCode;
    }

    public void setOrganDrugCode(String organDrugCode) {
        this.organDrugCode = organDrugCode;
    }

    @Column(name = "drug_id")
    public Integer getDrugId() {
        return drugId;
    }

    public void setDrugId(Integer drugId) {
        this.drugId = drugId;
    }

    @Column(name = "drug_type")
    public Integer getDrugType() {
        return drugType;
    }

    public void setDrugType(Integer drugType) {
        this.drugType = drugType;
    }

    @Column(name = "sort")
    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
