package com.ngari.recipe.regulation.service;

import com.ngari.recipe.regulation.model.QueryRegulationUnitDTO;
import com.ngari.recipe.regulation.model.RegulationDrugDeliveryDTO;
import com.ngari.recipe.regulation.model.RegulationRecipeDetailDTO;
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
    List<RegulationRecipeInfoDTO> queryRecipeInfos(QueryRegulationUnitDTO queryRegulationUnit);

    /**
     * 处方记录详情
     * @param queryRegulationUnit
     * @return 处方记录详情
     */
    @RpcService
    List<RegulationRecipeDetailDTO> queryRecipeDetails(QueryRegulationUnitDTO queryRegulationUnit);

    /**
     * 处方配送信息记录
     * @param queryRegulationUnit
     * @return 
     */
    @RpcService
    List<RegulationDrugDeliveryDTO> queryDrugDeliveries(QueryRegulationUnitDTO queryRegulationUnit);
}
