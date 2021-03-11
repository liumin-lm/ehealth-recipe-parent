package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Schema
@Table(name = "base_drugSources")
@Access(AccessType.PROPERTY)
public class DrugSources implements java.io.Serializable {

    private static final long serialVersionUID = -2304192004034649222L;

    @ItemProperty(alias = "来源序号  机构取机构ID")
    private Integer drugSourcesId;

    @ItemProperty(alias = "来源名称")
    private String drugSourcesName;

    public DrugSources() {

    }
    public DrugSources(Integer drugSourcesId, String drugSourcesName) {
        this.drugSourcesId = drugSourcesId;
        this.drugSourcesName = drugSourcesName;
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "drugSourcesId", unique = true, nullable = false)
    public Integer getDrugSourcesId() {
        return drugSourcesId;
    }

    public void setDrugSourcesId(Integer drugSourcesId) {
        this.drugSourcesId = drugSourcesId;
    }

    @Column(name = "drugSourcesName")
    public String getDrugSourcesName() {
        return drugSourcesName;
    }

    public void setDrugSourcesName(String drugSourcesName) {
        this.drugSourcesName = drugSourcesName;
    }
}
