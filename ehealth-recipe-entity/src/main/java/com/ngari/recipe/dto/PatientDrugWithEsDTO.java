package com.ngari.recipe.dto;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @description： 患者端搜索药品实体类
 * @author： whf
 * @date： 2021-08-23 14:25
 */
@Getter
@Setter
public class PatientDrugWithEsDTO implements Serializable {
    @ItemProperty(alias = "药品序号")
    private Integer drugId;

    @ItemProperty(alias = "机构药品编码")
    private String organDrugCode;

    @ItemProperty(alias = "药品名称")
    private String drugName;

    @ItemProperty(alias = "商品名")
    private String saleName;

    @ItemProperty(alias = "药品规格")
    private String drugSpec;

    @ItemProperty(alias = "药品单位")
    private String unit;

    @ItemProperty(alias = "生产厂家")
    private String producer;

    @ItemProperty(alias = "药品图片")
    private String drugPic;

}
