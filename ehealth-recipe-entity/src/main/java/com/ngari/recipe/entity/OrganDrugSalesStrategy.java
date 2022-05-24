package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

/**
 * 销售机构药品目录
 *
 * @author <a href="mailto:luf@ngarihealth.com">luf</a>
 */
@Data
public class OrganDrugSalesStrategy implements java.io.Serializable {
    private static final long serialVersionUID = -7090271704460035622L;

    @ItemProperty(alias = "id")
    private String id;

    @ItemProperty(alias = "策略标题")
    private String title;

    @ItemProperty(alias = "单位")
    private String unit;

    @ItemProperty(alias = "数量")
    private String amount;

    @ItemProperty(alias = "默认标识")
    private String isDefault;


}