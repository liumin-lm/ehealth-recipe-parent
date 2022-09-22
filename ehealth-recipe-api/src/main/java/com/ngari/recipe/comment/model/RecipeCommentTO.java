package com.ngari.recipe.comment.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.util.Date;

/**
 * Created by Administrator on 2022-09-17.
 */
@Data
public class RecipeCommentTO {
    @ItemProperty(alias = "主键")
    private Integer id;

    @ItemProperty(alias = "处方编号")
    private Integer recipeId;

    @ItemProperty(alias = "点评结果")
    private String commentResult;

    @ItemProperty(alias = "点评备注")
    private String commentRemark;

    @ItemProperty(alias = "创建时间")
    private Date createDate;

    @ItemProperty(alias = "最后修改时间")
    private Date lastmodify;
}
