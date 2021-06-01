package com.ngari.recipe.drug.service;

import com.ngari.recipe.drug.model.DrugListAndSaleDrugListDTO;
import com.ngari.recipe.drug.model.SaleDrugListDTO;
import ctd.persistence.bean.QueryResult;
import ctd.util.annotation.RpcService;

import java.util.Date;

/**
 * @Author liuzj
 * @Date 2020/3/16 17:25
 * @Description
 */
public interface ISaleDrugListService {

    @RpcService
    SaleDrugListDTO getByOrganIdAndDrugId(Integer enterprise, Integer drugId );

    @RpcService
    SaleDrugListDTO getByDrugId(Integer drugId );

    @RpcService
    QueryResult<DrugListAndSaleDrugListDTO> querySaleDrugListByOrganIdAndKeyword(Date startTime, Date endTime, Integer organId, String drugClass, String keyword, Integer status, Integer type, int start, int limit);

    @RpcService
    Long getCountByDrugId(int drugId);
}
