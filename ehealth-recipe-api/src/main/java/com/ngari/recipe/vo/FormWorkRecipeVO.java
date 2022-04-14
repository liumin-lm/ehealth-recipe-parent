package com.ngari.recipe.vo;

import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
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

    //模板ID
    private Integer mouldId;
    //模板标题
    private String title;
    //处方介绍
    private String introduce;
    //背景图片
    private String backgroundImg;
    //处方详情
    private RecipeBean recipeBean;
    //电子病例文本
    private String docText;
    //处方明细详情
    private List<RecipeDetailBean> detailBeanList;
}
