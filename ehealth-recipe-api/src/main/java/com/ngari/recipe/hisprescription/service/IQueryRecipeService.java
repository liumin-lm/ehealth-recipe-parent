package com.ngari.recipe.hisprescription.service;

import com.ngari.recipe.hisprescription.model.QueryRecipeReqDTO;
import com.ngari.recipe.hisprescription.model.QueryRecipeResultDTO;
import ctd.util.annotation.RpcService;

/**
 * 浙江互联网医院-----his查询处方详情
 * created by shiyuping on 2018/11/30
 */
public interface IQueryRecipeService {

    @RpcService
    QueryRecipeResultDTO queryRecipeInfo(QueryRecipeReqDTO queryRecipeReqDTO);
}
