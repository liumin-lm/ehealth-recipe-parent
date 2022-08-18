package com.ngari.recipe.dto;

import com.ngari.recipe.entity.RecipeBeforeOrder;
import com.ngari.recipe.entity.Recipedetail;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ShoppingCartDetailDTO implements Serializable {
    private static final long serialVersionUID = 4663398515404143885L;

    private RecipeBeforeOrder recipeBeforeOrder;

    private List<Recipedetail> recipeDetail;
}
