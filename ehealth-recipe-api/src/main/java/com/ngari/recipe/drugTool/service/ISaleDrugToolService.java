package com.ngari.recipe.drugTool.service;

import ctd.util.annotation.RpcService;

import java.util.Map;

/**
 * created by renfuhao on 2020/5/29
 */
public interface ISaleDrugToolService {

    @RpcService(timeout = 600)
    Map<String, Object> readDrugExcel(byte[] buf, String originalFilename, int organId, String operator, String ossId);
}
