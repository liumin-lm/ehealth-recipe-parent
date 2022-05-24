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

    @ItemProperty(alias = "")
    private String id;

    @ItemProperty(alias = "")
    private String title;

    @ItemProperty(alias = "")
    private String unit;

    @ItemProperty(alias = "")
    private String amount;

    @ItemProperty(alias = "")
    private String isDefault;


}