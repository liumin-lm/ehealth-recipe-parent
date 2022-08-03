package com.ngari.recipe.vo;

import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.schema.annotation.ItemProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 模板处方
 *
 * @author yinsheng
 * @date 2022\04\02 0021 09:24
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FormWorkRecipeVO implements Serializable {
    private static final long serialVersionUID = 2835067047332365247L;

    @ItemProperty(alias = "模板ID")
    private Integer mouldId;

    @ItemProperty(alias = "模板标题")
    private String title;

    @ItemProperty(alias = "处方介绍")
    private String introduce;

    @ItemProperty(alias = "背景图片")
    private String backgroundImg;

    @ItemProperty(alias = "处方详情")
    private RecipeBean recipeBean;

    @ItemProperty(alias = "电子病例文本")
    private String docText;

    @ItemProperty(alias = "处方明细详情")
    private List<RecipeDetailBean> detailBeanList;

    @ItemProperty(alias = "药方数量上限")
    private Integer maxNum;

}
