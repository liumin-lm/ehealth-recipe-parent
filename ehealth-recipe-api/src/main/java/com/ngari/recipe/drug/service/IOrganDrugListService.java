package com.ngari.recipe.drug.service;

import com.ngari.recipe.drug.model.RegulationOrganDrugListBean;
import ctd.util.annotation.RpcService;

import java.util.List;
import java.util.Map;

/**
 * @Author liuzj
 * @Date 2020/3/16 17:25
 * @Description
 */
public interface IOrganDrugListService {

    @RpcService
    List<RegulationOrganDrugListBean> queryRegulationDrugSHET(Map<String, Object> params);
}
