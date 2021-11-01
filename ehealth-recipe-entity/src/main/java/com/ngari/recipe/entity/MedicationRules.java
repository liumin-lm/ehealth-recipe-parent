package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 合理用药规则表
 * @author renfuhao
 */
@Entity
@Schema
@Table(name = "recipe_medication_rules")
@Access(AccessType.PROPERTY)
public class MedicationRules  implements Serializable {

    private static final long serialVersionUID = 486761631320053860L;

    @ItemProperty(alias = "合理用药规则Id")
    private Integer id;

    @ItemProperty(alias = "合理用药规则名称")
    private String medicationRulesName;

    @ItemProperty(alias = "规则适用处方类型")
    @Dictionary(id = "eh.cdr.dictionary.RecipeType")
    private Integer recipeType;

    @ItemProperty(alias = "规则 备注说明")
    private String descr;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "最后修改时间")
    private Date LastModify;


    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "medicationRulesName")
    public String getMedicationRulesName() {
        return medicationRulesName;
    }

    public void setMedicationRulesName(String medicationRulesName) {
        this.medicationRulesName = medicationRulesName;
    }

    @Column(name = "recipeType")
    public Integer getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(Integer recipeType) {
        this.recipeType = recipeType;
    }


    @Column(name = "descr")
    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
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
