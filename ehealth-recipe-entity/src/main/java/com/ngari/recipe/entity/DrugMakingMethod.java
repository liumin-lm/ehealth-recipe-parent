package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * cdr_drug_makeingMethod中药制法表
 */
@Entity
@Schema
@Table(name = "base_drug_makeingMethod")
@Access(AccessType.PROPERTY)
public class DrugMakingMethod implements Serializable {

    private static final long serialVersionUID = -7396436464542532302L;
    @ItemProperty(
        alias = "制法id"
    )
    private Integer methodId;

    @ItemProperty(
        alias = "机构编码"
    )
    private Integer organId;

    @ItemProperty(
        alias = "制法编码"
    )
    private String methodCode;

    @ItemProperty(
        alias = "制法名称"
    )
    private String methodText;

    @ItemProperty(
        alias = "制法拼音"
    )
    private String methodPingying;

    @ItemProperty(
        alias = "制法排序"
    )
    private Integer sort;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    public Integer getMethodId() {
        return methodId;
    }

    public void setMethodId(Integer methodId) {
        this.methodId = methodId;
    }

    public String getMethodCode() {
        return methodCode;
    }

    public void setMethodCode(String methodCode) {
        this.methodCode = methodCode;
    }

    public String getMethodText() {
        return methodText;
    }

    public void setMethodText(String methodText) {
        this.methodText = methodText;
    }

    public String getMethodPingying() {
        return methodPingying;
    }

    public void setMethodPingying(String methodPingying) {
        this.methodPingying = methodPingying;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }
}
