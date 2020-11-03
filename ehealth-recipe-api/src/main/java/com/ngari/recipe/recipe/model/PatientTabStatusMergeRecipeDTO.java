package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * created by shiyuping on 2020/10/30
 * 患者端处方列表页合并处方对象
 */
@Schema
@Data
public class PatientTabStatusMergeRecipeDTO implements Serializable {
    private static final long serialVersionUID = -2095398658115258241L;

    /**
     * 分组字段
     */
    private String groupField;

    private List<PatientTabStatusRecipeDTO> recipe;

    /**
     * 是否合并处方标识
     */
    private Boolean mergeRecipeFlag;

    /**
     * 第一个处方单id 排序用
     */
    private Integer firstRecipeId;

    /**
     * 合并支付的机构配置
     * e.registerId支持同一个挂号序号下的处方合并支付
     * e.registerId,e.chronicDiseaseName 支持同一个挂号序号且同一个病种的处方合并支付
     */
    private String mergeRecipeWay;
}
