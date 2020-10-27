package com.ngari.recipe.service;

import com.ngari.recipe.vo.ResultBean;
import com.ngari.recipe.vo.UpdateOrderStatusVO;

/**
 * @author fuzi
 */
public interface IRecipeOrderTwoService {
    ResultBean updateRecipeOrderStatus(UpdateOrderStatusVO updateOrderStatusVO);
}
