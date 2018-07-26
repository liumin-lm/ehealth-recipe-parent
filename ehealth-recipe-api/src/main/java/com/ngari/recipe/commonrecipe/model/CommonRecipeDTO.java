package com.ngari.recipe.commonrecipe.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.Column;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by  on 2017/5/22.
 *
 * @author jiangtingfeng
 */
@Schema
public class CommonRecipeDTO implements Serializable {

    private static final long serialVersionUID = -1946631470972113416L;

    @ItemProperty(alias = "医生身份ID")
    private Integer doctorId;

    @ItemProperty(alias = "常用方名称")
    private String commonRecipeName;

    @ItemProperty(alias = "常用方Id")
    private Integer commonRecipeId;

    @ItemProperty(alias = "处方类型")
    @Dictionary(id = "eh.cdr.dictionary.RecipeType")
    private Integer recipeType;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    @ItemProperty(alias = "机构代码")
    private Integer organId;

    @Column(name = "OrganId")
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public String getCommonRecipeName() {
        return commonRecipeName;
    }

    public void setCommonRecipeName(String commonRecipeName) {
        this.commonRecipeName = commonRecipeName;
    }

    public Integer getCommonRecipeId() {
        return commonRecipeId;
    }

    public void setCommonRecipeId(Integer commonRecipeId) {
        this.commonRecipeId = commonRecipeId;
    }

    public Integer getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(Integer recipeType) {
        this.recipeType = recipeType;
    }

    public Date getCreateDt() {
        return createDt;
    }

    public void setCreateDt(Date createDt) {
        this.createDt = createDt;
    }

    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }
}
