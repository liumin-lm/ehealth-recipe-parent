package com.ngari.recipe.drug.service;

import com.ngari.recipe.commonrecipe.model.RulesDrugCorrelationDTO;
import ctd.persistence.bean.QueryResult;
import ctd.util.annotation.RpcService;

import java.util.List;

public interface IRulesDrugCorrelationService {

    @RpcService
    QueryResult<RulesDrugCorrelationDTO> queryRulesDrugCorrelationByDrugCodeOrname(String input, int start, int limit);

    @RpcService
    void  deleteRulesDrugCorrelationById( Integer drugCorrelationId);

    @RpcService
    void addRulesDrugCorrelation(List<RulesDrugCorrelationDTO> lists, Integer rulesId);

    @RpcService
    Boolean  checkRulesDrugCorrelations( RulesDrugCorrelationDTO correlationDTO,Integer rulesId);
}
