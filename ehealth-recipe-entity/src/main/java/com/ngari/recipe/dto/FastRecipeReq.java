package com.ngari.recipe.dto;

import lombok.Data;

/**
 * @Description
 * @Author yzl
 * @Date 2022-08-17
 */
@Data
public class FastRecipeReq {

    private Integer organId;

    private Integer fastRecipeId;

    private Integer start;

    private Integer limit;
}
