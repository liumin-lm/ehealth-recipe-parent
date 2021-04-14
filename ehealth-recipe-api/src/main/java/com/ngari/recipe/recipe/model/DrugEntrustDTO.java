package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.util.Date;

/**
 * @author rfh
 */
@Schema
public class DrugEntrustDTO implements Serializable {
    private static final long serialVersionUID = 3558274129938280415L;


    @ItemProperty(alias = "药品嘱托ID")
    private  Integer drugEntrustId;

    @ItemProperty(alias = "药品嘱托编码")
    private String drugEntrustCode;

    @ItemProperty(alias = "药药品嘱托名称")
    private String drugEntrustName;

    @ItemProperty(alias = "药品嘱托 备注说明")
    private String drugEntrustValue;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "药品嘱托排序")
    private Integer sort;

    @ItemProperty(alias = "药品嘱托默认值标识")
    private Boolean drugEntrustDefaultFlag;

    @ItemProperty(alias = "机构ID")
    private Integer organId;

    public Boolean getDrugEntrustDefaultFlag() {
        return drugEntrustDefaultFlag;
    }

    public void setDrugEntrustDefaultFlag(Boolean drugEntrustDefaultFlag) {
        this.drugEntrustDefaultFlag = drugEntrustDefaultFlag;
    }

    public Integer getDrugEntrustId() {
        return drugEntrustId;
    }

    public void setDrugEntrustId(Integer drugEntrustId) {
        this.drugEntrustId = drugEntrustId;
    }

    public String getDrugEntrustCode() {
        return drugEntrustCode;
    }

    public void setDrugEntrustCode(String drugEntrustCode) {
        this.drugEntrustCode = drugEntrustCode;
    }

    public String getDrugEntrustName() {
        return drugEntrustName;
    }

    public void setDrugEntrustName(String drugEntrustName) {
        this.drugEntrustName = drugEntrustName;
    }

    public String getDrugEntrustValue() {
        return drugEntrustValue;
    }

    public void setDrugEntrustValue(String drugEntrustValue) {
        this.drugEntrustValue = drugEntrustValue;
    }

    public Date getCreateDt() {
        return createDt;
    }

    public void setCreateDt(Date createDt) {
        this.createDt = createDt;
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
