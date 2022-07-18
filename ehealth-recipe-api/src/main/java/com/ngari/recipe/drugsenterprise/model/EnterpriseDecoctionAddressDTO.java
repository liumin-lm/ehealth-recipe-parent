package com.ngari.recipe.drugsenterprise.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @description： 药企煎法地址
 * @author： whf
 * @date： 2022-04-07 11:18
 */
@Getter
@Setter
@Schema
public class EnterpriseDecoctionAddressDTO implements Serializable {

    @ItemProperty(alias = "药企地址序号")
    private Integer id;

    @ItemProperty(alias = "药企序号")
    private Integer enterpriseId;

    @ItemProperty(alias = "煎法序号")
    private Integer decoctionId;

    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "药企配送地址")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address;

    @ItemProperty(alias = "配送地址状态")
    private Integer status;
}
