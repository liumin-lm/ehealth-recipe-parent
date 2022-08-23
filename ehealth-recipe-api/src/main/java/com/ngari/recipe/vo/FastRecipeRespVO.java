package com.ngari.recipe.vo;

import lombok.Data;

import java.util.List;

/**
 * @Description
 * @Author yzl
 * @Date 2022-08-23
 */
@Data
public class FastRecipeRespVO {
    private List<FastRecipeVO> fastRecipeList;
    private Integer total;
}
