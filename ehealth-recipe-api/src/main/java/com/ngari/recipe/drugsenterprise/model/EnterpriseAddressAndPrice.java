package com.ngari.recipe.drugsenterprise.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @description：
 * @author： whf
 * @date： 2022-06-29 10:00
 */
@Getter
@Setter
public class EnterpriseAddressAndPrice implements Serializable {
    private static final long serialVersionUID = -9081127975058522117L;

    private Integer drugDistributionPriceId;
    @ItemProperty(alias = "药企地址序号")
    private Integer id;

    @ItemProperty(alias = "药企序号")
    private Integer enterpriseId;

    @ItemProperty(alias = "药企配送地址")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address;

    @ItemProperty(alias = "配送地址状态")
    private Integer status;

    @ItemProperty(alias = "配送价格")
    private BigDecimal distributionPrice;

    @ItemProperty(alias = "金额满多少包邮")
    private BigDecimal buyFreeShipping;

}
