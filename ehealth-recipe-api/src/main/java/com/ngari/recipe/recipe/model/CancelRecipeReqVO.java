package com.ngari.recipe.recipe.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author yinsheng
 * @date 2021\8\20 0020 09:00
 */
@Getter
@Setter
@AllArgsConstructor
public class CancelRecipeReqVO {

    private Integer busId;
    private String reason;
}
