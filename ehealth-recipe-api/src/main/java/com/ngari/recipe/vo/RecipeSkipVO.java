package com.ngari.recipe.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class RecipeSkipVO implements Serializable {
    private static final long serialVersionUID = -8643378036209343529L;

    private boolean showFlag;
    private String skipUrl;
}
