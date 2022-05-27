package com.ngari.recipe.vo;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class DrugSaleStrategyVO  implements Serializable {

    @ItemProperty(alias = "序号")
    private Integer id;

    @ItemProperty(alias = "药品ID")
    private Integer drugId;

    @ItemProperty(alias = "策略名称")
    private String strategyTitle;

    @ItemProperty(alias = "策略单位")
    private String drugUnit;

    @ItemProperty(alias = "销售系数比")
    private Integer drugAmount;

    @ItemProperty(alias = "状态 ，0：删除，1 正常")
    private Integer status;

    @ItemProperty(alias = "数据操作类型，add：新增 update:修改 delete:删除")
    private String type;
}
