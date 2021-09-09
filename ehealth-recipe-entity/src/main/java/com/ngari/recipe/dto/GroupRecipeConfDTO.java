package com.ngari.recipe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 处方合并支付配置
 * @author yinsheng
 * @date 2021\5\31 0031 10:53
 */
@Getter
@Setter
@AllArgsConstructor
public class GroupRecipeConfDTO {

    //合并支付标志
    private Boolean mergeRecipeFlag;
    //合并支付的方式
    private String mergeRecipeWayAfter;
}
