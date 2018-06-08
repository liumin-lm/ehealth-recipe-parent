package com.ngari.recipe.his.service;

import com.ngari.recipe.common.RecipeCommonReqTO;
import com.ngari.recipe.common.RecipeCommonResTO;
import ctd.util.annotation.RpcService;

/**
 *  @author： 0184/yu_yun
 *  @date： 2017/12/7
 *  @description： 处方相关对接HIS服务
 *  @version： 1.0
 */
public interface IRecipeHisService {

    /**
     * 复诊病人判断接口
     * @param request
     */
    @RpcService
    RecipeCommonResTO canVisit(RecipeCommonReqTO request);

    /**
     * 在线复诊挂号接口
     * @param request
     */
    @RpcService
    RecipeCommonResTO visitRegist(RecipeCommonReqTO request);

    /**
     * 在线复诊挂号取消接口
     * @param request
     */
    @RpcService
    RecipeCommonResTO cancelVisit(RecipeCommonReqTO request);

    /**
     * 在线复诊挂号记录接诊状态查询接口
     * @param request
     */
    @RpcService
    boolean queryVisitStatus(Integer consultId) ;


}
