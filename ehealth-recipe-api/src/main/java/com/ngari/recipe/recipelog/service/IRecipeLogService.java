package com.ngari.recipe.recipelog.service;

import com.ngari.recipe.IBaseService;
import com.ngari.recipe.recipelog.model.RecipeLogBean;
import ctd.util.annotation.RpcService;

/**
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/9/4.
 */
public interface IRecipeLogService extends IBaseService<RecipeLogBean> {

    /**
     * 保存日志
     * @param log
     */
    @RpcService
    void saveRecipeLogEx(RecipeLogBean log);

    /**
     * 保存日志
     * @param recipeId
     * @param beforeStatus
     * @param afterStatus
     * @param memo
     */
    @RpcService
    void saveRecipeLog(int recipeId, int beforeStatus, int afterStatus, String memo);
}
