package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author maoze
 * @description
 * @date 2021年10月12日 14:15
 */
@Entity
@Schema
@Table(name = "recipe_rules_drugcorrelation")
@Access(AccessType.PROPERTY)
public class RecipeRulesDrugcorrelation implements Serializable {

    private static final long serialVersionUID = -6170665419368031590L;

    @ItemProperty(alias = "主键ID")
    private Integer id;

    @ItemProperty(alias = "合理用药规则Id")
    private Integer medicationRulesId;

    @ItemProperty(alias = "合理用药规则Id")
    private Integer drugRelationship;

    @ItemProperty(alias = "规则药品编码")
    private Integer drugId;

    @ItemProperty(alias = "规则药品名称")
    private String drugName;

    @ItemProperty(alias = "规则药品名称")
    private Integer correlationDrugId;

    @ItemProperty(alias = "规则药品名称")
    private String correlationDrugName;

    @ItemProperty(alias = "最小规则药品 用量范围")
    private BigDecimal minimumDosageRange;

    @ItemProperty(alias = "最大规则药品 用量范围")
    private BigDecimal MaximumDosageRange;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "最后修改时间")
    private Date LastModify;

    @Id
    @GeneratedValue(strategy = IDENTITY)
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

    public void setMinimumDosageRange(BigDecimal minimumDosageRange) {
        this.minimumDosageRange = minimumDosageRange;
    }

    public void setMaximumDosageRange(BigDecimal maximumDosageRange) {
        MaximumDosageRange = maximumDosageRange;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }



    public String getCorrelationDrugName() {
        return correlationDrugName;
    }

    public void setCorrelationDrugName(String correlationDrugName) {
        this.correlationDrugName = correlationDrugName;
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
