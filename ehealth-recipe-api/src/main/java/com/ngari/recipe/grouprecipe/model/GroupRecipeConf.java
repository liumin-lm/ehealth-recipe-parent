package com.ngari.recipe.grouprecipe.model;

import lombok.Data;

/**
 * @author yinsheng
 * @date 2021\5\31 0031 10:53
 */
@Data
public class GroupRecipeConf {

    //合并支付标志
    private Boolean mergeRecipeFlag;
    //合并支付的方式
    private String mergeRecipeWayAfter;
}
