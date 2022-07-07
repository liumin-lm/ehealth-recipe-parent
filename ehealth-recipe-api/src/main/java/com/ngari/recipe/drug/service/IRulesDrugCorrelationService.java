package com.ngari.recipe.drug.service;

import ctd.persistence.bean.QueryResult;
import ctd.util.annotation.RpcService;
import recipe.vo.second.RecipeRulesDrugCorrelationVO;

import java.util.List;

public interface IRulesDrugCorrelationService {

    @RpcService
    QueryResult<RecipeRulesDrugCorrelationVO> queryRulesDrugCorrelationByDrugCodeOrname(Integer drugId, String input, Integer rulesId, int start, int limit);

    @RpcService
    void deleteRulesDrugCorrelationById(Integer drugCorrelationId);

    @RpcService
    void addRulesDrugCorrelation(List<RecipeRulesDrugCorrelationVO> recipeRulesList, Integer rulesId);

    @RpcService
    Boolean checkRulesDrugCorrelations(RecipeRulesDrugCorrelationVO correlationDTO, Integer rulesId);
}
