package com.ngari.recipe.drug.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * created by shiyuping on 2020/8/4
 */
@Data
public class SearchDrugDetailReqDTO implements Serializable {
    private static final long serialVersionUID = -3456826323972480907L;

    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "药品类型")
    private Integer drugType;

    @ItemProperty(alias = "品牌名、药品名、别名")
    private String drugName;

    @ItemProperty(alias = "药房id主键")
    private Integer pharmacyId;

    @ItemProperty(alias = "分页起始位置")
    private Integer start;
}
