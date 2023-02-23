package com.ngari.recipe.comment.service;

import com.ngari.platform.regulation.mode.QueryRegulationUnitReq;
import com.ngari.recipe.comment.model.RecipeCommentTO;
import com.ngari.recipe.comment.model.RegulationRecipeCommentBean;
import ctd.util.annotation.RpcService;

import java.util.List;

public interface IRecipeCommentService {
    /**
     * 新增点评
     *
     * @param recipeComment
     * @return
     */
    @RpcService
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
    @RpcService
    RecipeCommentTO getRecipeCommentByRecipeId(Integer recipeId);

    /**
     * 点评信息上传监管（前置机定时任务反查）
     *
     * @param req
     * @return
     */
    @RpcService
    List<RegulationRecipeCommentBean> queryRegulationRecipeComment(QueryRegulationUnitReq req);
}
