package com.ngari.recipe.comment.service;

import com.ngari.recipe.comment.model.RecipeCommentTO;
import ctd.util.annotation.RpcService;

import java.util.List;

public interface IRecipeCommentService {
    /**
     * 新增点评
     *
     * @param recipeComment
     * @return
     */
    Integer addRecipeComment(RecipeCommentTO recipeComment);

    /**
     * 查询点评列表
     *
     * @param recipeIds
     * @return
     */
    @RpcService
    List<RecipeCommentTO> findCommentByRecipeIds(List<Integer> recipeIds);

    /**
     * 根据recipeId查询点评列表
     *
     * @param recipeId
     * @return
     */
    RecipeCommentTO getRecipeCommentByRecipeId(Integer recipeId);
}
