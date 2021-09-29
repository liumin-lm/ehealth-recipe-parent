package com.ngari.recipe.pay.service;

import com.ngari.recipe.pay.model.PayResultDTO;
import ctd.util.annotation.RpcService;
import eh.entity.bus.Order;

import java.util.Map;

/**
 * 处方其他费用支付回调
 *
 * @author yinsheng
 */
public interface IRecipeOtherFeePayCallBackService {
    /**
     * 支付成功回调
     *
     * @param payResult payResult
     * @return boolean
     */
    @RpcService
    boolean doHandleAfterPay(PayResultDTO payResult);

    /**
     * 支付失败回调
     * @param payResult payResult
     * @return boolean
     */
    @RpcService
    boolean doHandleAfterPayFail(PayResultDTO payResult);

    /**
     * 退款成功或失败回调
     *
     * @param order order
     * @param targetPayFlag targetPayFlag
     * @param refundResult refundResult
     * @return boolean
     */
    @RpcService
    boolean doHandleAfterRefund(Order order, int targetPayFlag, Map<String, String> refundResult);
}
