package com.ngari.recipe.drug.service;

import com.ngari.recipe.drug.model.AuditDrugListDTO;
import com.ngari.recipe.drug.model.DrugListBean;
import ctd.persistence.bean.QueryResult;
import ctd.util.annotation.RpcService;

import java.util.List;

/**
 * @author yinsheng
 * @date 2019\5\15 0015 22:26
 */
public interface IAuditDrugListService {

    @RpcService
    AuditDrugListDTO getById(Integer auditDrugListId);

    @RpcService
    QueryResult<AuditDrugListDTO> findAllDrugList(Integer start, Integer limit);

    @RpcService
    QueryResult<AuditDrugListDTO> findAllDrugListByOrganId(Integer organId, Integer start, Integer limit);

    @RpcService
    List<DrugListBean> matchAllDrugListByName(String drugName);

    @RpcService
    void saveAuditDrugListInfo(Integer auditDrugListId, Integer drugListId);

    @RpcService
    void deleteAuditDrugListById(Integer auditDrugListId);

    @RpcService
    void hospitalAuditDrugList(Integer auditDrugListId, Double salePrice, Integer takeMedicine, Integer status, String rejectReason);
}
