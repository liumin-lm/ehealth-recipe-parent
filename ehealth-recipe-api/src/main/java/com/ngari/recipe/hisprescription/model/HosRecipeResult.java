package com.ngari.recipe.hisprescription.model;

import com.ngari.recipe.common.RecipeCommonResTO;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.util.JSONUtils;

import java.io.Serializable;

/**
 * 对接医院HIS结果对象
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * date:2017/4/18.
 */
public class HosRecipeResult extends RecipeCommonResTO implements Serializable {

    private static final long serialVersionUID = 2809725502013933071L;

    private String recipeCode;

    private Integer recipeId;

    private RecipeBean recipe;

    private HospitalRecipeDTO hospitalRecipe;

    public HosRecipeResult() {

    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public RecipeBean getRecipe() {
        return recipe;
    }

    public void setRecipe(RecipeBean recipe) {
        this.recipe = recipe;
    }

    public HospitalRecipeDTO getHospitalRecipe() {
        return hospitalRecipe;
    }

    public void setHospitalRecipe(HospitalRecipeDTO hospitalRecipe) {
        this.hospitalRecipe = hospitalRecipe;
    }

    @Override
    public String toString() {
        return JSONUtils.toString(this);
    }

}
