package com.ngari.recipe.vo;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class FastRecipeExtend implements Serializable {
    private static final long serialVersionUID = -8951912287654544548L;

    @ItemProperty(alias = "制法")
    private String makeMethodId;
    @ItemProperty(alias = "制法text")
    private String makeMethodText;
    @ItemProperty(alias = "每付取汁")
    private String juice;
    @ItemProperty(alias = "每付取汁单位")
    private String juiceUnit;
    @ItemProperty(alias = "次量")
    private String minor;
    @ItemProperty(alias = "次量单位")
    private String minorUnit;
    @ItemProperty(alias = "中医症候编码")
    private String symptomId;
    @ItemProperty(alias = "中医症候名称")
    private String symptomName;
    @ItemProperty(alias = "煎法")
    private String decoctionId;
    @ItemProperty(alias = "煎法text")
    private String decoctionText;
    @ItemProperty(alias = "煎法单价")
    private Double decoctionPrice;
}
