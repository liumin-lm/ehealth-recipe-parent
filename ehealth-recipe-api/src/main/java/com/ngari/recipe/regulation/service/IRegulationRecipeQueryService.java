package com.ngari.recipe.regulation.service;

import com.ngari.his.regulation.entity.RegulationChargeDetailReqTo;
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
    List<RegulationChargeDetailReqTo> queryRegulationChargeDetailList(QueryRegulationUnitReq queryRegulationUnit);

    /**
     * 查询未完成处方数
     * 处方业务这边规定在3天以内有效期且未支付的处方属于未完成的状态,处方业务需要提供这个接口给监管平台；
     *
     * @param organId 机构id
     * @return
     */
    @RpcService
    Integer findUnfinishedRecipe(Integer organId);
}
