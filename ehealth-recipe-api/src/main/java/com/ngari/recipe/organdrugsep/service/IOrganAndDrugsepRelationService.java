package com.ngari.recipe.organdrugsep.service;

import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.organdrugsep.model.OrganAndDrugsepRelationBean;
import ctd.util.annotation.RpcService;

import java.util.List;

public interface IOrganAndDrugsepRelationService {
    /**
     * 根据机构添加药企
     *
     * @param organId
     * @param entpriseIds
     * @return
     */
    @RpcService
    public List<OrganAndDrugsepRelationBean> addDrugEntRelationByOrganIdAndEntIds(Integer organId, List<Integer> entpriseIds);

    /**
     * 根据机构删除对应药企
     *
     * @param organId
     * @param entId
     */
    @RpcService
    public void deleteDrugEntRelationByOrganIdAndEntId(Integer organId, Integer entId);

    /**
     * 根据机构获取对应的药企列表
     *
     * @param organId 医院内码
     * @param status  药企状态
     * @return
     */
    @RpcService
    public List<DrugsEnterpriseBean> findDrugsEnterpriseByOrganId(final Integer organId, final Integer status);

}
