package com.ngari.recipe.drug.service;

import com.ngari.recipe.drug.model.*;
import ctd.persistence.bean.QueryResult;
import ctd.util.annotation.RpcService;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Author liuzj
 * @Date 2020/3/16 17:25
 * @Description
 */
public interface IOrganDrugListService {

    @RpcService
    List<RegulationDrugCategoryBean> queryRegulationDrug(Map<String, Object> params);

    @RpcService
    List<OrganDrugListBean> findByOrganIdAndDrugIdAndOrganDrugCode(int organId,
                                                                  int drugId, String organDrugCode);
    @RpcService(timeout = 600)
    QueryResult<DrugListAndOrganDrugListDTO> queryOrganDrugAndSaleForOp(OrganDrugQueryInfo info);

    @RpcService
    List<OrganDrugListBean> findByOrganId(int organId);

    @RpcService
    List<OrganDrugListBean> findByDrugIdAndOrganId(int drugId,int organId);

    /**
     * 根据医院药品编码 和机构编码查询 医院药品
     * @param organId
     * @param organDrugCode
     * @return
     */
    @RpcService
    OrganDrugListBean getByOrganIdAndOrganDrugCode( int organId, String organDrugCode);

    /**
     * 通过原来的药品id查询对照药品id
     * @param originalDrugId
     * @return
     */
    @RpcService
   Integer findTargetDrugIdByOriginalDrugId(Integer originalDrugId);

    @RpcService
    OrganDrugListBean getByOrganIdAndOrganDrugCodeAndDrugId(Integer organId,String organDrugId,Integer drugId);

    @RpcService
    List<OrganDrugListBean> findByDrugIdsAndOrganIds(List<Integer> drugIds,List<Integer> organIds);

    @RpcService
    Long getCountByDrugId(int drugId);
    @RpcService
    List<DepSaleDrugInfo> queryDepSaleDrugInfosByDrugId(final Integer organId,final Integer drugId);
}
