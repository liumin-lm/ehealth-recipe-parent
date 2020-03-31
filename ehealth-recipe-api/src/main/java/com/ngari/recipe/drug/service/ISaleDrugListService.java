package com.ngari.recipe.drug.service;

import com.ngari.recipe.drug.model.SaleDrugListDTO;
import ctd.util.annotation.RpcService;

/**
 * @Author liuzj
 * @Date 2020/3/16 17:25
 * @Description
 */
public interface ISaleDrugListService {

    @RpcService
    SaleDrugListDTO getByOrganIdAndDrugId(Integer enterprise, Integer drugId );
}
