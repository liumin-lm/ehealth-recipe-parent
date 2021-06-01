package com.ngari.recipe.service;

import ctd.persistence.bean.QueryResult;
import ctd.util.annotation.RpcService;

import java.util.Map;

/**
 * @author renfuhao
 */
public interface IShoppingService {

    @RpcService
    QueryResult<Map<String, Object>> findShoppingOrdersWithInfo(String bDate, String eDate, String mpiId, String orderCode,
                                                                Integer status, Integer start, Integer limit);
}
