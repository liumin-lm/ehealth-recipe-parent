package com.ngari.recipe.recipe.model;


import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import ctd.schema.annotation.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @author liumin
 */
@Setter
@Getter
@Schema
public class RecipeInfoBean implements Serializable {
    private static final long serialVersionUID = -9005230183318990440L;
    public RecipeBean recipe;
    public RecipeExtendBean recipeExtend;
    public List<RecipeDetailBean> recipeDetails;
    public RecipeOrderBean recipeOrder;
}
