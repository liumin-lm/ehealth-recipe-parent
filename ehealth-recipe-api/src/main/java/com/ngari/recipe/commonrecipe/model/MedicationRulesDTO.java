package com.ngari.recipe.commonrecipe.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.util.Date;

/**
 * 合理用药规则表
 *
 * @author renfuhao
 */
@Schema
public class MedicationRulesDTO implements java.io.Serializable {
    private static final long serialVersionUID = -8060738198333547119L;


    @ItemProperty(alias = "合理用药规则Id")
    private Integer medicationRulesId;

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

    public Integer getMedicationRulesId() {
        return medicationRulesId;
    }

    public void setMedicationRulesId(Integer medicationRulesId) {
        this.medicationRulesId = medicationRulesId;
    }

    public String getMedicationRulesName() {
        return medicationRulesName;
    }

    public void setMedicationRulesName(String medicationRulesName) {
        this.medicationRulesName = medicationRulesName;
    }

    public Integer getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(Integer recipeType) {
        this.recipeType = recipeType;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
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
