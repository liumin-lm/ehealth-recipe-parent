package com.ngari.recipe.dto;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author zgy
 * @date 2022/6/28 11:10
 */
@Data
public class AdvanceWarningReqDTO implements Serializable {

    @ItemProperty(alias = "处方单号")
    private Integer recipeId;

    @ItemProperty(alias = "端标识符 0：PC 1：App")
    private Integer serverFlag;
}
