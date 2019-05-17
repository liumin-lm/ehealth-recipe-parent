package com.ngari.recipe.drug.service;

import com.ngari.recipe.drug.model.AuditDrugListDTO;
import ctd.util.annotation.RpcService;

import java.util.List;

/**
 * @author yinsheng
 * @date 2019\5\15 0015 22:26
 */
public interface IAuditDrugListService {

    @RpcService
    void updateAuditDrugListStatus(Integer auditDrugListId, Integer status, String rejectReason);

    @RpcService
    List<AuditDrugListDTO> findAllDrugList(Integer start, Integer limit);

    @RpcService
    List<AuditDrugListDTO> findAllDrugListByOrganId(Integer organId, Integer start, Integer limit);
}
