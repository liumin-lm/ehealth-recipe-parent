package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author zgy
 * @date 2022/6/28 11:05
 */
@Data
public class AdvanceWarningReqVO implements Serializable {

    @ItemProperty(alias = "处方单号")
    private Integer recipeId;

    @ItemProperty(alias = "端标识符 0：PC 1：App")
    private Integer serverFlag;



}
