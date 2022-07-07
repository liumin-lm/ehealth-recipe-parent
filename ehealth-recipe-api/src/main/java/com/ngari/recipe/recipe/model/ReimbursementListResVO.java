package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 查询报销清单列表出参、查询报销清单详情入参
 * @author zgy
 * @date 2022/6/7 11:04
 */
@Data
public class ReimbursementListResVO implements Serializable {

    private static final long serialVersionUID = 9114181874235704583L;

    @ItemProperty(alias="处方单号")
    private Integer recipeId;

    @ItemProperty(alias="姓名")
    private String name;

    @ItemProperty(alias="性别")
    private String sex;

    @ItemProperty(alias="年龄")
    private Integer age;

    @ItemProperty(alias="发票号")
    private String invoiceNumber;

    @ItemProperty(alias="支付时间")
    private Date payTime;

    @ItemProperty(alias="医保属性 ")
    private String medicalFlag;

    private List<RecipeDetailBean> recipeDetail;

}
