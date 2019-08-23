package com.ngari.recipe.hisprescription.service;

import com.ngari.recipe.hisprescription.model.QueryRecipeReqDTO;
import com.ngari.recipe.hisprescription.model.QueryRecipeResultDTO;
import com.ngari.recipe.hisprescription.model.RegulationRecipeIndicatorsDTO;
import ctd.util.annotation.RpcService;

import java.util.Date;
import java.util.List;

/**
 * his查询处方详情--反查接口
 * created by shiyuping on 2018/11/30
 */
public interface IQueryRecipeService {
    /**
     * 浙江省互联网医院his查询处方接口
     * @param queryRecipeReqDTO
     * @return
     */
    @RpcService
    QueryRecipeResultDTO queryRecipeInfo(QueryRecipeReqDTO queryRecipeReqDTO);

    /**
     * 监管平台处方反查接口
     * @param organId
     * @param startDate
     * @param endDate
     * @param checkFlag
     * @return
     */
    @RpcService
    List<RegulationRecipeIndicatorsDTO> queryRegulationRecipeData(Integer organId, Date startDate, Date endDate, Boolean checkFlag);
}
