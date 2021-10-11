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
    private Integer rulesDrugCorrelationId;


    @ItemProperty(alias = "合理用药规则Id")
    private Integer medicationRulesId;

    @ItemProperty(alias = "药品关系")
    @Dictionary(id = "eh.cdr.dictionary.DrugRelationship")
    private Integer drugRelationship;

    @ItemProperty(alias = "规则药品编码")
    private String drugCode;

    @ItemProperty(alias = "规则药品名称")
    private String drugName;

    @ItemProperty(alias = "规则关联药品编码")
    private String correlationDrugCode;

    @ItemProperty(alias = "规则关联药品名称")
    private String correlationDrugName;

    @ItemProperty(alias = "最小规则药品 用量范围  中药药品超量规则 用")
    private Double minimumDosageRange;

    @ItemProperty(alias = "最大规则药品 用量范围  中药药品超量规则 用")
    private Double MaximumDosageRange;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "最后修改时间")
    private Date LastModify;

    public Integer getRulesDrugCorrelationId() {
        return rulesDrugCorrelationId;
    }

    public void setRulesDrugCorrelationId(Integer rulesDrugCorrelationId) {
        this.rulesDrugCorrelationId = rulesDrugCorrelationId;
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

    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getCorrelationDrugCode() {
        return correlationDrugCode;
    }

    public void setCorrelationDrugCode(String correlationDrugCode) {
        this.correlationDrugCode = correlationDrugCode;
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

    public Double getMaximumDosageRange() {
        return MaximumDosageRange;
    }

    public void setMaximumDosageRange(Double maximumDosageRange) {
        MaximumDosageRange = maximumDosageRange;
    }

    public Date getCreateDt() {
        return createDt;
    }

    public void setCreateDt(Date createDt) {
        this.createDt = createDt;
    }

    public Date getLastModify() {
        return LastModify;
    }

    public void setLastModify(Date lastModify) {
        LastModify = lastModify;
    }
}
