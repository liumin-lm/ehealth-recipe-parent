package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

/**
 * 药企药品目录销售策略
 *
 * @author liumin
 */
@Data
public class SaleDrugSalesStrategy implements java.io.Serializable {
    private static final long serialVersionUID = -7090271704460035622L;
  
    @ItemProperty(alias = "关联机构销售策略id")
    private String organDrugListSalesStrategyId;

    @ItemProperty(alias = "单位")
    private String unit;

    @ItemProperty(alias = "开关是否开启")
    private String buttonIsOpen;

    @ItemProperty(alias = "是否默认销售策略")
    private String isDefault;


}