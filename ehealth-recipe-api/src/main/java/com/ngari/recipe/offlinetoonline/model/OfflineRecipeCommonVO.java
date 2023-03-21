package com.ngari.recipe.offlinetoonline.model;

import com.ngari.recipe.recipe.model.*;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author liumin
 * @Date 2023/3/21 上午11:42
 * @Description 线下开方通用vo （线上复诊线下开方，线下门诊线下开方）
 */
/**
 * @Author liumin
 * @Date 2021/1/26 上午11:42
 * @Description 线下转线上获取详情返回参数
 */
@SuppressWarnings("ALL")
@Data
public class OfflineRecipeCommonVO implements Serializable {
    private static final long serialVersionUID = -7727248592234567484L;

    /**
     * 线下
     */
    private HisRecipeBean hisRecipe;

    private HisRecipeExt hisRecipeExt;

    private List<HisRecipeDetailBean> hisRecipeDetails;


    /**
     * 线上
     */
    private RecipeBean recipe;

    private RecipeExtendBean recipeExtend;

    private List<RecipeDetailBean> recipeDetails;

    private RecipeOrderBean recipeOrder;


}


