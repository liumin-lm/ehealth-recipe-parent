package com.ngari.recipe.recipe.model;

import lombok.Data;

/**
 * Created by Administrator on 2022-08-25.
 */
@Data
public class RecipeOrderExportQueryTO {

    private RecipeExportDTO recipeExportDTO;

    private RecipeOrderExportDTO recipeOrderExportDTO;

    private RecipeDetailExportDTO recipeDetailExportDTO;
}
