package com.ngari.recipe.regulation.service;

import com.ngari.his.regulation.entity.RegulationChargeDetailReq;
import com.ngari.platform.regulation.mode.QueryRegulationUnitReq;
import ctd.util.annotation.RpcService;

import java.util.List;

/**
 * 监管平台查询处方数据
 * 2020/03/16
 */
public interface IRegulationRecipeQueryService {

    /**
     * 互联网服务收费明细
     */
    @RpcService
   List<RegulationChargeDetailReq> queryRegulationChargeDetailList(QueryRegulationUnitReq queryRegulationUnit);
}
