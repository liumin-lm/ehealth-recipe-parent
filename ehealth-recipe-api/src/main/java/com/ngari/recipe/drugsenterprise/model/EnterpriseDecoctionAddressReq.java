package com.ngari.recipe.drugsenterprise.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @description： 药企煎法配送请求入参
 * @author： whf
 * @date： 2022-04-07 14:00
 */
@Getter
@Setter
public class EnterpriseDecoctionAddressReq implements Serializable {
    @ItemProperty(alias = "药企序号")
    private Integer enterpriseId;

    @ItemProperty(alias = "煎法序号")
    private Integer decoctionId;

    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "区域编码")
    private String area;

    @ItemProperty(alias = "药企配送地址合集")
    private List<EnterpriseDecoctionAddressDTO> enterpriseDecoctionAddressDTOS;
}
