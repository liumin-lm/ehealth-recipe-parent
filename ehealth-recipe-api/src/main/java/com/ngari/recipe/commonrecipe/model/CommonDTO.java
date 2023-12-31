package com.ngari.recipe.commonrecipe.model;

import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * 常用方集合对象
 *
 * @author fuzi
 */
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommonDTO implements Serializable {
    private static final long serialVersionUID = -5082249144585320804L;
    /**
     * 常用方头数据
     */
    private CommonRecipeDTO commonRecipeDTO;
    /**
     * 常用方扩展
     */
    private CommonRecipeExtDTO commonRecipeExt;
    /**
     * 常用方药品
     */
    private List<CommonRecipeDrugDTO> commonRecipeDrugList;
}
