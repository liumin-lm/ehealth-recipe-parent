package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 查询报销清单详情出参
 * @author zgy
 * @date 2022/6/8 9:16
 */
@Data
public class ReimbursementDetailResVO implements Serializable {

    private static final long serialVersionUID = -3782969788980003252L;

    @ItemProperty(alias="姓名")
    private String name;

    @ItemProperty(alias="门诊号码")
    private String patientId;

    @ItemProperty(alias="发票号")
    private String invoiceNumber;

    @ItemProperty(alias="支付时间")
    private Date payTime;

    @ItemProperty(alias="医保属性 ")
    private String medicalFlag;

    @ItemProperty(alias = "处方类型 1 西药 2 中成药 3 中药 4膏方")
    private Integer recipeType;

    //项目详情
    private List<RecipeDetailBean> recipeDetailList;
}
