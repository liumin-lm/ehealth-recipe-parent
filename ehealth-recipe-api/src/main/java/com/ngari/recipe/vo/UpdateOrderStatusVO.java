package com.ngari.recipe.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 修改订单状态VO
 *
 * @author fuzi
 */
@Getter
@Setter
public class UpdateOrderStatusVO implements Serializable {
    private static final long serialVersionUID = -7727248592234567484L;
    /**
     * 处方号
     */
    private Integer recipeId;
    /**
     * 订单id
     */
    private Integer orderId;
    /**
     * 目标订单状态
     */
    private Integer targetRecipeOrderStatus;

    /**
     * 源订单状态
     */
    private Integer sourceRecipeOrderStatus;
    /**
     * 物流单号
     */
    private String trackingNumber;
    /**
     * 物流公司
     */
    private Integer logisticsCompany;
    /**
     * 配送人
     */
    private String sender;

    /**
     * 目标处方状态
     */
    private Integer targetRecipeStatus;

    /**
     * 源处方状态
     */
    private Integer sourceRecipeStatus;
}


