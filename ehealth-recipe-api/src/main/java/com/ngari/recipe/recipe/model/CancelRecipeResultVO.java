package com.ngari.recipe.recipe.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 处方撤销结果
 * @author yinsheng
 * @date 2021\8\20 0020 08:58
 */
@Getter
@Setter
@AllArgsConstructor
public class CancelRecipeResultVO {

    private boolean cancelResult;
    private String cancelErrorReason;
}
