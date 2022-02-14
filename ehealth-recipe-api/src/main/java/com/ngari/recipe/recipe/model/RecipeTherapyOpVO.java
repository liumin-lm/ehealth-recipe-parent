package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 运营平台诊疗处方列表
 * @author zgy
 * @date 2021/12/31 16:35
 */
@Getter
@Setter
public class RecipeTherapyOpVO implements Serializable {
    private static final long serialVersionUID = -6990095333192024627L;

    @ItemProperty(alias = "处方id")
    private Integer recipeId;

    @ItemProperty(alias = "处方单号")
    private String recipeCode;

    @ItemProperty(alias = "患者姓名")
    private String patientName;

    @ItemProperty(alias = "患者电话")
    private String patientMobile;

    @ItemProperty(alias = "医生姓名")
    private String doctorName;

    @ItemProperty(alias = "挂号科室名称")
    private String appointDepartName;

    @ItemProperty(alias = "开方机构名称")
    private String organName;

    @ItemProperty(alias = "诊疗处方状态")
    private Integer status;

    @ItemProperty(alias = "开具时间")
    private String createTime;

}
