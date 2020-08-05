package com.ngari.recipe.recipe.service;

import com.ngari.recipe.recipe.model.PharmacyTcmDTO;
import ctd.persistence.bean.QueryResult;
import ctd.util.annotation.RpcService;

import java.util.List;

public interface IPharmacyTcmService {

    /**
     *新增药房
     * @param pharmacyTcm
     * @return
     */
    @RpcService
    boolean addPharmacyTcmForOrgan(PharmacyTcmDTO pharmacyTcm);

    /**
     *根据机构ID和药房名称查询药房
     * @param organId
     * @param input
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    QueryResult<PharmacyTcmDTO> querPharmacyTcmByOrganIdAndName(Integer organId , String input, final int start, final int limit);

    /**
     *根据机构id查询药房
     * @param organId
     * @return
     */
    @RpcService
    List<PharmacyTcmDTO> querPharmacyTcmByOrganId(Integer organId );
}
