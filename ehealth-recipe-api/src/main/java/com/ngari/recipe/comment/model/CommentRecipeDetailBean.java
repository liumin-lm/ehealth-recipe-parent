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
public class CommentRecipeDetailBean implements Serializable {
    private static final long serialVersionUID = -2639419624205356479L;

    private String drugId;

    private String drugCode;

    private String hospDrugCode;

    private String drugName;

    private String drugSpec;
    private String drugPack;
    private String drugPackUnit;
    private String drugPlace;

    @ItemProperty(alias = "药品用法")
    private String drugUsage;

    private String drugFrequency;

    @ItemProperty(alias = "药品剂量")
    private String drugDose;

    @ItemProperty(alias = "药品批次")
    private String drugBatch;

    @ItemProperty(alias = "用药总量")
    private String useTotalDose;

    @ItemProperty(alias = "用药总量")
    private String useTotalDoseUnit;

    @ItemProperty(alias = "用药天数")
    private Integer useDays;

    @ItemProperty(alias = "是否OTC, 0:不是，1:是")
    private Integer otcMark;

    @ItemProperty(alias = "药品备注")
    private String remark;
}
