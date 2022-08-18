package com.ngari.recipe.dto;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class ShoppingCartReqDTO implements Serializable {
    private static final long serialVersionUID = -4861632352670503609L;

    @ItemProperty(alias = "处方单号")
    private Integer recipeId;

    @ItemProperty(alias = "药企ID")
    private Integer enterpriseId;

    @ItemProperty(alias = "取药药店或站点名称")
    private String drugStoreName;

    @ItemProperty(alias = "取药药店或站点编码")
    private String drugStoreCode;

    @ItemProperty(alias = "取药药店或站点地址")
    private String drugStoreAddr;

    @ItemProperty(alias = "配送地址id")
    private Integer addressId;

    @ItemProperty(alias = "购药方式")
    private Integer giveMode;

    @ItemProperty(alias = "支付方式")
    private String payWay;

    @ItemProperty(alias = "操作人mpiId")
    private String operMpiId;

    @ItemProperty(alias = "0 无 1 药店取药，2 站点取药")
    private Integer takeMedicineWay;

    @ItemProperty(alias = "订单所属配送方式")
    private String giveModeKey;
}
