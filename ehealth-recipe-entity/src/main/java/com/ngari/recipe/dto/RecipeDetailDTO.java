package com.ngari.recipe.dto;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class RecipeDetailDTO implements Serializable {
    @ItemProperty(alias = "药品ID")
    private Integer drguId;

    @ItemProperty(alias = "处方详情编码")
    private String recipeDeatilCode;

    @ItemProperty(alias = "药品编码")
    private String drugCode;

    @ItemProperty(alias = "药品名称")
    private String drugName;

    @ItemProperty(alias = "药品价格")
    private BigDecimal price;

    @ItemProperty(alias = "药品数量")
    private Double amount;

    @ItemProperty(alias = "药品总价")
    private BigDecimal totalPrice;

    @ItemProperty(alias = "药品规格")
    private String drModel;

    @ItemProperty(alias = "频次名称")
    private String usingRate;

    @ItemProperty(alias = "用法名称")
    private String usePathWays;

    @ItemProperty(alias = "机构的频次代码")
    private String usingRateCode;

    @ItemProperty(alias = "机构的用药途径代码")
    private String usePathwaysCode;

    @ItemProperty(alias = "剂量单位")
    private String useDoseUnit;

    @ItemProperty(alias = "药品剂量")
    private String useDose;

    @ItemProperty(alias = "药品剂量 特殊用法")
    private String useDoseStr;

    @ItemProperty(alias = "药品单位")
    private String unit;

    @ItemProperty(alias = "用药天数")
    private String days;

    @ItemProperty(alias = "")
    private String remark;

    @ItemProperty(alias = "商品名")
    private String saleName;

    @ItemProperty(alias = "药品规格")
    private String drugSpec;

    @ItemProperty(alias = "批准文号")
    private String licenseNumber;

    @ItemProperty(alias = "药品本位码")
    private String standardCode;

    @ItemProperty(alias = "药品生产厂家")
    private String producer;

    @ItemProperty(alias = "厂家编码")
    private String producerCode;

    @ItemProperty(alias = "开药总数")
    private BigDecimal useTotalDose;

    @ItemProperty(alias = "包装单位")
    private String drugUnit;

    @ItemProperty(alias = "用药频率说明")
    private String usingRateText;

    @ItemProperty(alias = "用药方式说明")
    private String usePathwaysText;

    @ItemProperty(alias = "药品使用备注，如:饭前，饭后")
    private String memo;

    @ItemProperty(alias = "是否可流转 0 不可在互联网流转 1 可以流转")
    private Integer status;

    @ItemProperty(alias = "用药天数")
    private Integer useDays;

    @ItemProperty(alias = "包装数量")
    private Integer pack;

    @ItemProperty(alias = "用药天数（小数类型的）")
    private String useDaysB;

    @ItemProperty(alias = "药房编码")
    private String pharmacyCode;

    @ItemProperty(alias = "药房名称")
    private String pharmacyName;

    @ItemProperty(alias = "药房类型")
    private String pharmacyCategray;

    @ItemProperty(alias = "中药禁忌类型(1:超量 2:十八反 3:其它)")
    private Integer tcmContraindicationType;

    @ItemProperty(alias = "中药禁忌原因")
    private String tcmContraindicationCause;

    @ItemProperty(alias = "药品嘱托编码")
    private String drugEntrustCode;

    @ItemProperty(alias = "剂型")
    private String drugForm;

    @ItemProperty(alias = "药品拼接名称")
    private String drugDisplaySplicedName;


}