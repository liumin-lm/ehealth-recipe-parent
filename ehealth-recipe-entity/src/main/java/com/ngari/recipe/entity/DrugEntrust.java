package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 药品嘱托
 * @author renfuhao
 */
@Entity
@Schema
@Table(name = "recipe_drugentrust")
@Access(AccessType.PROPERTY)
public class DrugEntrust implements Serializable {

    private static final long serialVersionUID = 7767698218629127672L;

    @ItemProperty(alias = "药品嘱托ID")
    private  Integer drugEntrustId;

    @ItemProperty(alias = "药品嘱托编码")
    private String drugEntrustCode;

    @ItemProperty(alias = "药品嘱托名称")
    private String drugEntrustName;

    @ItemProperty(alias = "药品嘱托 备注说明")
    private String drugEntrustValue;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "药品嘱托排序")
    private Integer sort;

    @ItemProperty(alias = "机构ID")
    private Integer organId;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "drugEntrustId", unique = true, nullable = false)
    public Integer getDrugEntrustId() {
        return drugEntrustId;
    }

    public void setDrugEntrustId(Integer drugEntrustId) {
        this.drugEntrustId = drugEntrustId;
    }

    @Column(name = "drugEntrustCode")
    public String getDrugEntrustCode() {
        return drugEntrustCode;
    }

    public void setDrugEntrustCode(String drugEntrustCode) {
        this.drugEntrustCode = drugEntrustCode;
    }

    @Column(name = "drugEntrustName")
    public String getDrugEntrustName() {
        return drugEntrustName;
    }

    public void setDrugEntrustName(String drugEntrustName) {
        this.drugEntrustName = drugEntrustName;
    }

    @Column(name = "drugEntrustValue")
    public String getDrugEntrustValue() {
        return drugEntrustValue;
    }

    public void setDrugEntrustValue(String drugEntrustValue) {
        this.drugEntrustValue = drugEntrustValue;
    }

    @Column(name = "createDt")
    public Date getCreateDt() {
        return createDt;
    }

    public void setCreateDt(Date createDt) {
        this.createDt = createDt;
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
}
