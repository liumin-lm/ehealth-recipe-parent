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

    @ItemProperty(alias = "ID")
    private Integer id;

    @ItemProperty(alias = "来源序号  机构取机构ID")
    private Integer drugSourcesId;

    @ItemProperty(alias = "来源名称")
    private String drugSourcesName;

    public DrugSources() {

    }

    public DrugSources(Integer id, Integer drugSourcesId, String drugSourcesName) {
        this.id = id;
        this.drugSourcesId = drugSourcesId;
        this.drugSourcesName = drugSourcesName;
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

    @Column(name = "drugSourcesId")
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
