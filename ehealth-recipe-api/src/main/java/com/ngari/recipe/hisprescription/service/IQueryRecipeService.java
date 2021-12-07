package com.ngari.recipe.hisprescription.service;

import com.ngari.platform.recipe.mode.OrganDrugChangeBean;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.hisprescription.model.*;
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

    @RpcService
    QueryRecipeResultDTO queryPlatRecipeByRecipeId(QueryPlatRecipeInfoByDateDTO req);

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


    @RpcService
    List<DrugListBean> getDrugList(String organId, String organName, Integer start, Integer limit);

    @RpcService
    RecipeResultBean updateOrSaveOrganDrug(OrganDrugChangeBean organDrugChangeBean);

    @RpcService
    Boolean updateSuperviseRecipecodeToRecipe(Integer recipeId, String superviseRecipecode);


    @RpcService
    RecipeOrderBillDTO getRecipeOrderBill(Integer recipeId);

    /**
     * 医院数据中心处方信息上传
     * @param organId
     * @param startDate
     * @param endDate
     * @return
     */
    @RpcService
    public List<QueryRecipeInfoDTO> queryRecipeDataForHisDataCenter(Integer organId, Date startDate, Date endDate);

}
