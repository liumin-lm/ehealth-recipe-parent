package com.ngari.recipe.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 快捷购药处方入参
 */
@Getter
@Setter
public class FastRecipeReqVO implements Serializable {
    private static final long serialVersionUID = -2435387378914570888L;

    private String title;
    private String backgroundImg;
    private String introduce;
    private Integer recipeId;
}
