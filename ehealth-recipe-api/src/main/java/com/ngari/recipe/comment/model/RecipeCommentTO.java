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

    @ItemProperty(alias = "点评结果编码：0:不通过，1：通过")
    private Integer commentResultCode;

    @ItemProperty(alias = "点评结果")
    private String commentResult;

    @ItemProperty(alias = "点评备注")
    private String commentRemark;

    @ItemProperty(alias = "点评人姓名")
    private String commentUserName;

    @ItemProperty(alias = "点评人urt")
    private String commentUserUrt;

    @ItemProperty(alias = "点评人类型")
    private String commentUserType;

    @ItemProperty(alias = "创建时间")
    private Date createDate;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;
}
