package com.ngari.recipe.comment.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @Description
 * @Author yzl
 * @Date 2023-02-22
 */
@Data
public class CommentRecipeBean implements Serializable {
    private static final long serialVersionUID = 4997001479155860437L;

    private Integer recipeId;

    private String recipeCode;

    private String visitId;

    @ItemProperty(alias = "平台科室Id")
    private String departmentId;

    @ItemProperty(alias = "行政科室编码")
    private String departmentCode;

    @ItemProperty(alias = "行政科室名称")
    private String departmentName;

    @ItemProperty(alias = "挂号科室编码")
    private String appointDepartmentCode;

    @ItemProperty(alias = "挂号科室名称")
    private String appointDepartmentName;

    @ItemProperty(alias = "诊断 ICD 码")
    private String icdCode;

    @ItemProperty(alias = "初步诊断名称")
    private String icdName;
}
