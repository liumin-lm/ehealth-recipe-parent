package com.ngari.recipe.vo;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author zgy
 * @Date 2022-10-26
 */
@Data
public class RecipeSkipReqVO implements Serializable {

    private static final long serialVersionUID = -4196498932898936323L;

    @ItemProperty(alias = "机构Id")
    private Integer organId;

    @ItemProperty(alias = "处方编号")
    private String recipeCode;

    @ItemProperty(alias = "类型 1 线上处方 2 门诊处方")
    private Integer recipeType;
}
