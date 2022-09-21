package com.ngari.recipe.drugsenterprise.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @description：
 * @author： whf
 * @date： 2022-04-07 15:07
 */
@Getter
@Setter
public class EnterpriseDecoctionList implements Serializable {

    @ItemProperty(alias = "煎法序号")
    private Integer decoctionId;

    @ItemProperty(alias = "煎法名称")
    private String decoctionName;

    @ItemProperty(alias = "是否已配置 0：未配置, 1：部分地区已配置, 2：全部地区已配置")
    private Integer status;
}
