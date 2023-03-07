package com.ngari.recipe.drugsenterprise.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 药企配送地址
 *
 * @company: Ngarihealth
 * @author: yins
 * @date:2023/3/3.
 */
@Data
public class EnterpriseAddressVO implements java.io.Serializable {

    private static final long serialVersionUID = 1119475189827008619L;

    @ItemProperty(alias = "药企appKey")
    private String appKey;

    @ItemProperty(alias = "药企序号")
    private Integer enterpriseId;

    @ItemProperty(alias = "药企配送地址")
    private String address;

    @ItemProperty(alias = "配送地址状态")
    private Integer status;

    @ItemProperty(alias = "配送价格")
    private BigDecimal distributionPrice;

    @ItemProperty(alias = "金额满多少包邮")
    private BigDecimal buyFreeShipping;

}
