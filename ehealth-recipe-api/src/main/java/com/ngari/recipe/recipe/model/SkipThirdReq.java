package com.ngari.recipe.recipe.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * @author yinsheng
 * @date 2021\6\18 0018 16:24
 */
@Getter
@Setter
@NoArgsConstructor
public class SkipThirdReq {

    /**
     * 处方集合
     */
    private List<Integer> recipeIds;
    /**
     * 患者选择的购药方式
     */
    private Integer giveMode;
}
