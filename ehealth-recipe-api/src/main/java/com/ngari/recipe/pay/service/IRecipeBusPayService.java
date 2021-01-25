package com.ngari.recipe.pay.service;

import com.ngari.recipe.pay.model.WnExtBusCdrRecipeDTO;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
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
    ConfirmOrder obtainConfirmOrder(String busType, Integer busId, Map<String, String> extInfo);

    /**
     * 获取业务信息
     *
     * @param busId
     * @return
     */
    SimpleBusObject getSimpleBusObject(Integer busId);

    /**
     * 下单时保存平台订单号等信息
     *
     * @param order 订单数据
     */
    void onOrder(Order order);

    /**
     * 检查该业务是否可以继续支付，按需实现
     *
     * @param busId 业务id
     */
    boolean checkCanPay(Integer busId);

    /**
     * 组装卫宁付支付请求入参
     *
     * @param recipeOrder
     * @param patient
     * @return
     */
    WnExtBusCdrRecipeDTO newWnExtBusCdrRecipe(RecipeOrderBean recipeOrder, Patient patient);
}
