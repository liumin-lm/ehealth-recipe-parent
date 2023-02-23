package com.ngari.recipe.comment.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @Description
 * @Author yzl
 * @Date 2023-02-22
 */
@Data
public class RegulationRecipeCommentBean implements Serializable {

    private static final long serialVersionUID = -4268993828971678728L;

    private String unitId;

    private Integer organId;

    private String organName;

    @ItemProperty(alias = "处方信息")
    private CommentRecipeBean recipeMsg;

    @ItemProperty(alias = "医生信息")
    private CommentDoctorBean doctorMsg;

    @ItemProperty(alias = "点评信息")
    private CommentBean commentMsg;

    @ItemProperty(alias = "患者信息")
    private CommentPatientBean patientMsg;

    @ItemProperty(alias = "药品详情")
    private List<CommentRecipeDetailBean> recipeDetailList;

    @ItemProperty(alias = "汇总的单抗菌药数量")
    private Integer antibacterialDrugNum;

    @ItemProperty(alias = "汇总的激素药数量")
    private Integer hormoneNum;

    @ItemProperty(alias = "汇总的注射剂数量")
    private Integer injectionNum;

    @ItemProperty(alias = "汇总的通用名数量")
    private Integer commonNameNum;

    @ItemProperty(alias = "汇总的国家基础药品种数")
    private Integer baseDrugNum;

    @ItemProperty(alias = "数据更新时间")
    private Date updateTime;

}
