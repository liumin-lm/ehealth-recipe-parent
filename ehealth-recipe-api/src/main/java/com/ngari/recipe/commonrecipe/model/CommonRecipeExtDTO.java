package com.ngari.recipe.commonrecipe.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;


/**
 * 常用方扩展对象
 *
 * @author fuzi
 */
@Setter
@Getter
@Schema
public class CommonRecipeExtDTO implements Serializable {

    private static final long serialVersionUID = 2099193306225724304L;
    @ItemProperty(alias = "常用方Id")
    private Integer commonRecipeId;
    @ItemProperty(alias = "制法")
    private String makeMethodId;
    @ItemProperty(alias = "制法text")
    private String makeMethodText;
    @ItemProperty(alias = "制法")
    private String makeMethod;
    @ItemProperty(alias = "煎法")
    private String decoctionId;
    @ItemProperty(alias = "煎法text")
    private String decoctionText;
    @ItemProperty(alias = "煎法")
    private String decoctionCode;

    @ItemProperty(alias = "每付取汁")
    private String juice;
    @ItemProperty(alias = "每付取汁单位")
    private String juiceUnit;
    @ItemProperty(alias = "次量")
    private String minor;
    @ItemProperty(alias = "次量单位")
    private String minorUnit;
    @ItemProperty(alias = "剂数")
    private Integer copyNum;
    @ItemProperty(alias = "嘱托")
    private String entrust;
}
