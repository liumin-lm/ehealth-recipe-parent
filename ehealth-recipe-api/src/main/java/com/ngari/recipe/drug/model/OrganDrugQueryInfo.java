package com.ngari.recipe.drug.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.util.Date;

@Schema
public class OrganDrugQueryInfo implements Serializable {

    private static final long serialVersionUID = -2926228283625505958L;

    @ItemProperty(alias = "开始时间")
    private Date startTime;
    @ItemProperty(alias = "截止时间")
    private Date endTime;
    @ItemProperty(alias = "机构ID")
    private Integer organId;
    @ItemProperty(alias = "药品分类")
    private String drugClass;
    @ItemProperty(alias = "输入")
    private String keyword;
    @ItemProperty(alias = "药品状态")
    private Integer status;
    @ItemProperty(alias = "是否关联监管平台药品")
    private Integer isregulationDrug;
    @ItemProperty(alias = "药品类型")
    private Integer type;
    @ItemProperty(alias = "开始")
    private Integer start;
    @ItemProperty(alias = "条数")
    private Integer limit;
    @ItemProperty(alias = "是否支持配送")
    private Boolean canDrugSend;
    @ItemProperty(alias = "厂家")
    private String produce;

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public String getDrugClass() {
        return drugClass;
    }

    public void setDrugClass(String drugClass) {
        this.drugClass = drugClass;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getIsregulationDrug() {
        return isregulationDrug;
    }

    public void setIsregulationDrug(Integer isregulationDrug) {
        this.isregulationDrug = isregulationDrug;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Boolean getCanDrugSend() {
        return canDrugSend;
    }

    public void setCanDrugSend(Boolean canDrugSend) {
        this.canDrugSend = canDrugSend;
    }

    public String getProduce() {
        return produce;
    }

    public void setProduce(String produce) {
        this.produce = produce;
    }
}
