package com.ngari.recipe.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
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
     * 目标状态
     */
    private Integer target;
    /**
     * 物流单号
     */
    private String trackingNumber;
    /**
     * 物流公司
     */
    private String logisticsCompany;
}
