package com.ngari.recipe.commonrecipe.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.util.Date;

/**
 * 合理用药规则 药品关系表
 *
 * @author renfuhao
 */
@Schema
public class RulesDrugCorrelationDTO  implements java.io.Serializable {

    private static final long serialVersionUID = -7235240394983438859L;

    @ItemProperty(alias = "规则关联药品表ID")
    private Integer id;


    @ItemProperty(alias = "合理用药规则Id")
    private Integer medicationRulesId;

    @ItemProperty(alias = "药品关系")
    @Dictionary(id = "eh.cdr.dictionary.DrugRelationship")
    private Integer drugRelationship;

    @ItemProperty(alias = "规则药品Id")
    private Integer drugId;

    @ItemProperty(alias = "规则药品名称")
    private String drugName;

    @ItemProperty(alias = "规则关联药品Id")
    private Integer correlationDrugId;

    @ItemProperty(alias = "规则关联药品名称")
    private String correlationDrugName;

    @ItemProperty(alias = "最小规则药品 用量范围  中药药品超量规则 用")
    private Double minimumDosageRange;

    @ItemProperty(alias = "最大规则药品 用量范围  中药药品超量规则 用")
    private Double maximumDosageRange;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getMedicationRulesId() {
        return medicationRulesId;
    }

    public void setMedicationRulesId(Integer medicationRulesId) {
        this.medicationRulesId = medicationRulesId;
    }

    public Integer getDrugRelationship() {
        return drugRelationship;
    }

    public void setDrugRelationship(Integer drugRelationship) {
        this.drugRelationship = drugRelationship;
    }


    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public Integer getDrugId() {
        return drugId;
    }

    public void setDrugId(Integer drugId) {
        this.drugId = drugId;
    }

    public Integer getCorrelationDrugId() {
        return correlationDrugId;
    }

    public void setCorrelationDrugId(Integer correlationDrugId) {
        this.correlationDrugId = correlationDrugId;
    }

    public String getCorrelationDrugName() {
        return correlationDrugName;
    }

    public void setCorrelationDrugName(String correlationDrugName) {
        this.correlationDrugName = correlationDrugName;
    }

    public Double getMinimumDosageRange() {
        return minimumDosageRange;
    }

    public void setMinimumDosageRange(Double minimumDosageRange) {
        this.minimumDosageRange = minimumDosageRange;
    }


    public Date getCreateDt() {
        return createDt;
    }

    public void setCreateDt(Date createDt) {
        this.createDt = createDt;
    }

    public Double getMaximumDosageRange() {
        return maximumDosageRange;
    }

    public void setMaximumDosageRange(Double maximumDosageRange) {
        this.maximumDosageRange = maximumDosageRange;
    }

    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }
}
