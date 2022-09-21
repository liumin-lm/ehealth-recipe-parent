package com.ngari.recipe.drugTool.service;

import ctd.util.annotation.RpcService;
import recipe.vo.greenroom.ImportDrugRecordVO;

import java.util.Map;

/**
 * liumin
 */
public interface IOrganDrugToolService {

    /**
     * 药品
     * @param id
     * @return
     */
    @RpcService(timeout = 6000)
    Map<String, Object> readDrugExcel(Integer id);

    /**
     * 导入药品记录保存
     * @param importDrugRecord
     * @return
     */
    @RpcService
    Integer saveImportDrugRecord(ImportDrugRecordVO importDrugRecord);







}
