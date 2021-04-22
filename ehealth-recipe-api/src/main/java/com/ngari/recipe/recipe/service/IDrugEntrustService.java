package com.ngari.recipe.recipe.service;

import com.ngari.recipe.recipe.model.DrugEntrustDTO;
import ctd.persistence.bean.QueryResult;

import java.util.List;

public interface IDrugEntrustService {

    DrugEntrustDTO getDrugEntrustForId(Integer pharmacyTcmId);


    QueryResult<DrugEntrustDTO> querDrugEntrustByOrganIdAndName(Integer organId , String input, Integer start, Integer limit);

    List<DrugEntrustDTO> querDrugEntrustByOrganId(Integer organId );
}
