package com.ngari.recipe.dto;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * @author zgy
 * @date 2022/1/4 9:52
 */
@Getter
@Setter
public class RecipeTherapyOpDTO implements Serializable {

    private static final long serialVersionUID = 6345081400589732478L;

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
    private Date createTime;
}
