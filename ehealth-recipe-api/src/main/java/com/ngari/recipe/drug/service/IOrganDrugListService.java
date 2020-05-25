package com.ngari.recipe.drug.service;

import com.ngari.recipe.drug.model.DrugListAndOrganDrugListDTO;
import com.ngari.recipe.drug.model.OrganDrugListBean;
import com.ngari.recipe.drug.model.RegulationDrugCategoryBean;
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
    @RpcService
    QueryResult<DrugListAndOrganDrugListDTO> queryOrganDrugAndSaleForOp(Date startTime, Date endTime, Integer organId, String drugClass, String keyword, Integer status, int start, int limit, Boolean canDrugSend);
}
