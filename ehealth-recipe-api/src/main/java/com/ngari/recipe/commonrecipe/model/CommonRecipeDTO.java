package com.ngari.recipe.commonrecipe.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by  on 2017/5/22.
 * 常用方
 *
 * @author jiangtingfeng
 */
@Schema
@Getter
@Setter
public class CommonRecipeDTO implements Serializable {

    private static final long serialVersionUID = -1946631470972113416L;

    @ItemProperty(alias = "常用方编码-医院唯一主键字段")
    private String commonRecipeCode;

    @ItemProperty(alias = "常用方类型：1平台，2协定方，3保密方 4。。。")
    private Integer commonRecipeType;

    @ItemProperty(alias = "医生身份ID")
    private Integer doctorId;

    @ItemProperty(alias = "常用方名称")
    private String commonRecipeName;

    @ItemProperty(alias = "常用方Id")
    private Integer commonRecipeId;

    @ItemProperty(alias = "处方类型")
    @Dictionary(id = "eh.cdr.dictionary.RecipeType")
    private Integer recipeType;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    @ItemProperty(alias = "机构代码")
    private Integer organId;

    //开当前处方的配置项信息
    @ItemProperty(alias = "可开长处方按钮状态、长处方开药天数、非长处方开药天数")
    private String recipeJsonConfig;

    @ItemProperty(alias = "药房id主键")
    private Integer pharmacyId;
    @ItemProperty(alias = "药房名称")
    private String pharmacyName;
    @ItemProperty(alias = "药房编码")
    private String pharmacyCode;
    @ItemProperty(alias = "失效药品id列表")
    private List<Integer> drugIdList;
    @ItemProperty(alias = "常用方的状态")
    private Integer commonRecipeStatus;
    @ItemProperty(alias = "是否是长处方")
    private String isLongRecipe;
    @ItemProperty(alias = "机构的用法代码")
    private String organUsePathways;
    @ItemProperty(alias = "机构的用法代码名称")
    private String organUsePathwaysName;


    @ItemProperty(alias = "药品列表")
    @Deprecated
    private List<CommonRecipeDrugDTO> commonDrugList;
    @ItemProperty(alias = "常用方扩展信息")
    @Deprecated
    private CommonRecipeExtDTO commonRecipeExt;


    @ItemProperty(alias = "返回药品状态 0:正常，1已失效，2未完善")
    private Integer validateStatus;

    @ItemProperty(alias = "处方剂型 1 饮片方 2 颗粒方")
    private Integer recipeDrugForm;
}
