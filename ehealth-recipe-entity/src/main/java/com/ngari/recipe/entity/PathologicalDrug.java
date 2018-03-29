package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/5/26.
 */
@Entity
@Schema
@Table(name = "cdr_pathological_drug")
@Access(AccessType.PROPERTY)
public class PathologicalDrug implements Serializable {

    @ItemProperty(alias = "自增编号")
    private Integer id;

    @ItemProperty(alias = "药品分类")
    @Dictionary(id="eh.cdr.dictionary.PathologicalType")
    private Integer pathologicalType;

    @ItemProperty(alias = "药品ID")
    private Integer drugId;

    @ItemProperty(alias = "机构ID")
    private Integer organId;

    @ItemProperty(alias = "排序")
    private Integer sort;

    public PathologicalDrug(){}

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "Id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "PathologicalType")
    public Integer getPathologicalType() {
        return pathologicalType;
    }

    public void setPathologicalType(Integer pathologicalType) {
        this.pathologicalType = pathologicalType;
    }

    @Column(name = "DrugId")
    public Integer getDrugId() {
        return drugId;
    }

    public void setDrugId(Integer drugId) {
        this.drugId = drugId;
    }

    @Column(name = "OrganId")
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Column(name = "Sort")
    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
