package com.ngari.recipe.drugsenterprise.model;

import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.recipe.model.RecipeBean;

public class ThirdResultBean extends RecipeResultBean {

    private RecipeBean recipe;

    private String recipeCode;

    public ThirdResultBean(Integer code) {
        setCode(code);
    }

    public RecipeBean getRecipe() {
        return recipe;
    }

    public void setRecipe(RecipeBean recipe) {
        this.recipe = recipe;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public static ThirdResultBean getSuccess() {
        return new ThirdResultBean(SUCCESS);
    }

    public static ThirdResultBean getFail() {
        return new ThirdResultBean(FAIL);
    }
}