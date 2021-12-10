package com.ngari.recipe.recipe.service;

import com.ngari.recipe.recipe.model.DrugEntrustDTO;
import ctd.persistence.bean.QueryResult;
import ctd.util.annotation.RpcService;

import java.util.List;

public interface IDrugEntrustService {

    @RpcService
    DrugEntrustDTO getDrugEntrustForId(Integer pharmacyTcmId);


    @RpcService
    QueryResult<DrugEntrustDTO> querDrugEntrustByOrganIdAndName(Integer organId , String input, Integer start, Integer limit);

    @RpcService
    List<DrugEntrustDTO> querDrugEntrustByOrganId(Integer organId );
}
