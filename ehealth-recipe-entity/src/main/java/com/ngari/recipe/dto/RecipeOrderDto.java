package com.ngari.recipe.dto;

import com.ngari.recipe.entity.Recipe;
import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @description： 订单列表出参
 * @author： whf
 * @date： 2021-11-08 14:19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeOrderDto implements Serializable {

    @ItemProperty(alias = "订单ID")
    private Integer orderId;

    @ItemProperty(alias = "订单编号")
    private String orderCode;

    @ItemProperty(alias = "患者编号")
    private String mpiId;

    @ItemProperty(alias = "订单状态")
    private Integer status;

    @ItemProperty(alias = "订单状态")
    private Integer statusText;

    @ItemProperty(alias = "订单总费用")
    private BigDecimal totalFee;

    @ItemProperty(alias = "订单下处方详情")
    private List<RecipeBeanDTO> recipeList;
}
