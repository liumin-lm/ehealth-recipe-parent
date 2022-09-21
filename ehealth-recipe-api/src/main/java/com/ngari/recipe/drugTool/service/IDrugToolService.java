package com.ngari.recipe.drugTool.service;

import ctd.util.annotation.RpcService;
import recipe.vo.greenroom.ImportDrugRecordVO;

import java.util.Map;

/**
 * created by shiyuping on 2019/2/14
 */
public interface IDrugToolService {

    @Deprecated
    @RpcService(timeout = 6000)
    Map<String, Object> readDrugExcel(byte[] buf, String originalFilename, int organId, String operator);

    @RpcService
    Boolean judgePlatformDrugDelete(int drugId);

}
