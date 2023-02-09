package com.ngari.recipe.vo;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 合并物流单
 */
@Getter
@Setter
public class LogisticsMergeVO implements Serializable {
    private static final long serialVersionUID = 919522455711228364L;

    @ItemProperty(alias = "是否合并物流")
    private Boolean logisticsMergeFlag;

    @ItemProperty(alias = "物流公司")
    @Dictionary(id = "eh.infra.dictionary.LogisticsCode")
    private Integer logisticsCompany;

    @ItemProperty(alias = "物流公司名称")
    private String logisticsCompanyName;
}
