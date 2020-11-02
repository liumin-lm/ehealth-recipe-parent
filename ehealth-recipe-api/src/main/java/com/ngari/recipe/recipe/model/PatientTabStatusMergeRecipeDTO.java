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
     * 挂号序号
     */
    private String registerId;

    /**
     * 慢病病种
     */
    private String chronicDiseaseName;

    private List<PatientTabStatusRecipeDTO> recipe;

    /**
     * 是否合并处方标识
     */
    private Boolean mergeRecipeFlag;

    /**
     * 第一个处方单id 排序用
     */
    private Integer firstRecipeId;
}
