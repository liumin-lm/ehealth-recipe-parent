package com.ngari.recipe.vo;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PreOrderInfoReqVO implements Serializable {
    private static final long serialVersionUID = 2086197326291172280L;

    @ItemProperty(alias = "处方单号")
    private List<Integer> recipeId;

    @ItemProperty(alias = "取药药店或站点名称")
    private String drugStoreName;

    @ItemProperty(alias = "取药药店或站点编码")
    private String drugStoreCode;

    @ItemProperty(alias = "取药药店或站点地址")
    private String drugStoreAddr;

    @ItemProperty(alias = "配送地址id")
    private Integer addressId;
}
