package com.ngari.recipe.commonrecipe.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 线下常用方集合对象
 * @author fuzi
 */
@Setter
@Getter
public class CommonDTO implements Serializable {
    private static final long serialVersionUID = -5082249144585320804L;
    private CommonRecipeDTO commonRecipeDTO;
    private CommonRecipeExtDTO commonRecipeExt;
    private List<CommonRecipeDrugDTO> commonRecipeDrugList;
}
