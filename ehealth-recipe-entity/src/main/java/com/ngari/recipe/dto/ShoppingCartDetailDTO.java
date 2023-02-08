package com.ngari.recipe.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ShoppingCartDetailDTO implements Serializable {
    private static final long serialVersionUID = 4663398515404143885L;

    private RecipeBeforeOrderDTO recipeBeforeOrder;

    private List<RecipeDTO> recipeDTO;
}
