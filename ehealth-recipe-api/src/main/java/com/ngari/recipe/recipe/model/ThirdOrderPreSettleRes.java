package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @description： 第三方订单预算出参
 * @author： whf
 * @date： 2022-08-15 11:42
 */
@Data
public class ThirdOrderPreSettleRes implements Serializable {
    private static final long serialVersionUID = 7133436525748377861L;

    @ItemProperty(alias = "处方预结算返回支付总金额")
    private String preSettleTotalAmount;

    @ItemProperty(alias = "处方预结算返回医保支付金额")
    private String fundAmount;

    @ItemProperty(alias = "处方预结算返回自费金额")
    private String cashAmount;
}
