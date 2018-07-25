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
public interface IRecipeToHisService {

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
     * 在线复诊挂号是否成功接口
     * @param consultId
     */
    @RpcService
    RecipeCommonResTO visitRegistSuccess(Integer consultId);

    /**
     * 取消复诊挂号接口
     * @param Integer
     */
    @RpcService
    RecipeCommonResTO cancelVisit(Integer consultId);

    /**
     * 在线复诊挂号记录接诊状态查询接口
     * @param request
     */
    @RpcService
    RecipeCommonResTO queryVisitStatus(Integer consultId);

    /**
     * 取消HIS挂号记录失败的记录再次发起取消
     * @return
     */
    @RpcService
    void cancelVisitForFail();

}
