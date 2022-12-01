package com.ngari.recipe.recipe.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @description： 暂存处方入参
 * @author： whf
 * @date： 2022-11-30 14:47
 */
@Getter
@Setter
public class StagingRecipeReq implements Serializable {
    private static final long serialVersionUID = -4763123768183681696L;
    /**
     * 处方实体类
     */
    private RecipeBean recipeBean;
    /**
     * 药品详情实体类
     */
    private List<RecipeDetailBean> detailBeanList;

}
