package com.ngari.recipe.pay.service;

import com.ngari.recipe.pay.model.WnExtBusCdrRecipeDTO;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import ctd.util.annotation.RpcService;
import eh.entity.bus.Order;
import eh.entity.bus.pay.ConfirmOrder;
import eh.entity.bus.pay.SimpleBusObject;
import eh.entity.mpi.Patient;

import java.util.Map;

/**
 * created by shiyuping on 2021/1/22
 * 处方业务支付请求接口
 */
public interface IRecipeBusPayService {
    /**
     * 获取确认订单页信息
     *
     * @param busType 业务类型
     * @param busId   业务id
     * @param extInfo 扩展信息
     * @return
     */
    @RpcService
    ConfirmOrder obtainConfirmOrder(String busType, Integer busId, Map<String, String> extInfo);

    /**
     * 获取业务信息
     *
     * @param busId
     * @return
     */
    @RpcService
    SimpleBusObject getSimpleBusObject(Integer busId);

    /**
     * 获取处方审方费用(邵逸夫模式)
     *
     * @param busId
     * @return
     */
    @RpcService
    SimpleBusObject getRecipeAuditSimpleBusObject(Integer busId);

    /**
     * 下单时保存平台订单号等信息
     *
     * @param order 订单数据
     */
    @RpcService
    void onOrder(Order order);

    /**
     * 检查该业务是否可以继续支付，按需实现
     *
     * @param busId 业务id
     */
    @RpcService
    boolean checkCanPay(Integer busId);

    /**
     * 组装卫宁付支付请求入参
     *
     * @param recipeOrder
     * @param patient
     * @return
     */
    @RpcService
    WnExtBusCdrRecipeDTO newWnExtBusCdrRecipe(RecipeOrderBean recipeOrder, Patient patient);
}
