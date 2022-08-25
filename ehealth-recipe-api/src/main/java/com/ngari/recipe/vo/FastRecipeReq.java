package com.ngari.recipe.vo;

import lombok.Data;
import recipe.vo.PageVO;

import java.util.List;

/**
 * @Description
 * @Author yzl
 * @Date 2022-08-17
 */
@Data
public class FastRecipeReq extends PageVO {

    private Integer organId;

    private Integer fastRecipeId;

    private List<Integer> statusList;
}
