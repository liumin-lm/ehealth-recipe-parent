package com.ngari.recipe.service;

import com.ngari.recipe.vo.ResultBean;
import com.ngari.recipe.vo.UpdateOrderStatusVO;

/**
 * @author fuzi
 */
public interface IRecipeOrderTwoService {
    /**
     * 订单状态更新
     *
     * @param updateOrderStatusVO
     * @return
     */
    ResultBean<Boolean> updateRecipeOrderStatus(UpdateOrderStatusVO updateOrderStatusVO);
}
