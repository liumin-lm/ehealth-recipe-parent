package com.ngari.recipe.drug.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * cdr_drug_makeingMethod中药制法表
 */
@Schema
public class DrugMakingMethodBean implements Serializable {

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
