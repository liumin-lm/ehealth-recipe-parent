package com.ngari.recipe.dto;

import com.ngari.recipe.entity.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 第三方下载处方订单
 *
 * @author yins
 */
@Getter
@Setter
public class DownLoadRecipeOrderDTO implements Serializable {
    private static final long serialVersionUID = -6156870660796349493L;

    private RecipeOrder recipeOrder;
    private List<Recipe> recipeList;
    private List<RecipeExtend> recipeExtendList;
    private List<Recipedetail> recipeDetailList;
    private List<SaleDrugList> saleDrugLists;

}
