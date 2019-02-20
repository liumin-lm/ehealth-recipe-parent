package com.ngari.recipe.drugTool.service;

import ctd.util.annotation.RpcService;

/**
 * created by shiyuping on 2019/2/14
 */
public interface IDrugToolService {

    @RpcService
    void readDrugExcel(byte[] buf, String originalFilename, int organId, String operator);
}
