package com.ngari.recipe.regulation.service;

import com.ngari.recipe.regulation.model.QueryRegulationUnitDTO;
import com.ngari.recipe.regulation.model.RegulationRecipeInfoDTO;
import ctd.util.annotation.RpcService;

import java.util.List;

/**
 * 监管平台查询处方数据
 * 2020/03/16
 */
public interface IRegulationRecipeQueryService {

    /**
     * 处方记录表
     * @param queryRegulationUnit
     * @return 处方记录信息
     */

    @RpcService
    List<RegulationRecipeInfoDTO> queryRecipeInfo(QueryRegulationUnitDTO queryRegulationUnit);

}
