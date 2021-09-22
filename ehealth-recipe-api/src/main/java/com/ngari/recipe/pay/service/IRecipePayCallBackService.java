package com.ngari.recipe.pay.service;

import com.ngari.recipe.pay.model.PayResultDTO;
import ctd.util.annotation.RpcService;
import eh.entity.bus.Order;

import java.util.Map;

/**
 * created by shiyuping on 2021/1/22
 * 处方支付回调接口
 */
public interface IRecipePayCallBackService {
    /**
     * 支付成功回调
     *
     * @param payResult
     * @return
     */
    @RpcService
    boolean doHandleAfterPay(PayResultDTO payResult);

    /**
     * 支付失败回调
     * @param payResult
     * @return
     */
    @RpcService
    boolean doHandleAfterPayFail(PayResultDTO payResult);

    /**
     * 退款成功或失败回调
     *
     * @param order
     * @param targetPayflag
     * @param refundResult
     * @return
     */
    @RpcService
    boolean doHandleAfterRefund(Order order, int targetPayflag, Map<String, String> refundResult);
}
