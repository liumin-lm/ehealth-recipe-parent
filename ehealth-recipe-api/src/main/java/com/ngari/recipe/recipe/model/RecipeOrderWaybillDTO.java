package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @description： 订单运单信息
 * @author： whf
 * @date： 2022-06-27 13:44
 */
@Data
public class RecipeOrderWaybillDTO implements Serializable {
    private static final long serialVersionUID = -7911217958266693327L;

    @ItemProperty(alias = "订单ID")
    private Integer orderId;

    @ItemProperty(alias = "订单编号")
    private String orderCode;

    @ItemProperty(alias = "物流公司")
    @Dictionary(id = "eh.infra.dictionary.LogisticsCode")
    private Integer logisticsCompany;

    @ItemProperty(alias = "快递单号")
    private String trackingNumber;

    @ItemProperty(alias = "创建时间")
    private Date createTime;
}
