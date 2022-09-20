package com.ngari.recipe.comment.service;

import com.ngari.recipe.comment.model.RecipeCommentTO;
import ctd.util.annotation.RpcService;

import java.util.List;

/**
 * created by shiyuping on 2018/11/26
 */
public interface IRecipeCommentService {

    @RpcService
    List<RecipeCommentTO> findCommentByRecipeIds(List<Integer> recipeIds);

}
