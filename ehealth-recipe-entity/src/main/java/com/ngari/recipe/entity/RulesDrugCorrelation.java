package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 合理用药规则 药品关系表
 * @author renfuhao
 */
@Entity
@Schema
@Table(name = "recipe_rules_drugCorrelation")
@Access(AccessType.PROPERTY)
public class RulesDrugCorrelation  implements Serializable {

    private static final long serialVersionUID = -1918938219429895165L;

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


    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "rulesDrugCorrelationId", unique = true, nullable = false)
    public Integer getRulesDrugCorrelationId() {
        return rulesDrugCorrelationId;
    }

    public void setRulesDrugCorrelationId(Integer rulesDrugCorrelationId) {
        this.rulesDrugCorrelationId = rulesDrugCorrelationId;
    }

    @Column(name = "medicationRulesId")
    public Integer getMedicationRulesId() {
        return medicationRulesId;
    }

    public void setMedicationRulesId(Integer medicationRulesId) {
        this.medicationRulesId = medicationRulesId;
    }

    @Column(name = "drugRelationship")
    public Integer getDrugRelationship() {
        return drugRelationship;
    }

    public void setDrugRelationship(Integer drugRelationship) {
        this.drugRelationship = drugRelationship;
    }

    @Column(name = "drugCode")
    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    @Column(name = "drugName")
    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    @Column(name = "correlationDrugCode")
    public String getCorrelationDrugCode() {
        return correlationDrugCode;
    }

    public void setCorrelationDrugCode(String correlationDrugCode) {
        this.correlationDrugCode = correlationDrugCode;
    }

    @Column(name = "correlationDrugName")
    public String getCorrelationDrugName() {
        return correlationDrugName;
    }

    public void setCorrelationDrugName(String correlationDrugName) {
        this.correlationDrugName = correlationDrugName;
    }

    @Column(name = "minimumDosageRange")
    public Double getMinimumDosageRange() {
        return minimumDosageRange;
    }

    public void setMinimumDosageRange(Double minimumDosageRange) {
        this.minimumDosageRange = minimumDosageRange;
    }

    @Column(name = "MaximumDosageRange")
    public Double getMaximumDosageRange() {
        return MaximumDosageRange;
    }

    public void setMaximumDosageRange(Double maximumDosageRange) {
        MaximumDosageRange = maximumDosageRange;
    }

    @Column(name = "createDt")
    public Date getCreateDt() {
        return createDt;
    }

    public void setCreateDt(Date createDt) {
        this.createDt = createDt;
    }

    @Column(name = "LastModify")
    public Date getLastModify() {
        return LastModify;
    }

    public void setLastModify(Date lastModify) {
        LastModify = lastModify;
    }
}
