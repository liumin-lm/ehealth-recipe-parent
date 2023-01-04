package com.ngari.recipe.recipeorder.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @company: ngarihealth
 * @author: liumin
 * @date:20111223
 */
@Schema
@Data
public class RecipeOrderVoNoDS extends RecipeOrderBean implements Serializable {

    private static final long serialVersionUID = -1365227235362189226L;

    @ItemProperty(alias = "药企电话")
    private String tel;

    @ItemProperty(alias = "收货人手机号")
    private String recMobile;

    @ItemProperty(alias = "详细地址")
    private String address4;

    @ItemProperty(alias = "医保标识 0 自费 1 医保")
    private Integer medicalInsuranceFlag;

    @ItemProperty(alias = "物流状态")
    private String logisticsStateText;
}
