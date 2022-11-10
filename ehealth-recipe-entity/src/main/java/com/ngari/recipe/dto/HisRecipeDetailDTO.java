package com.ngari.recipe.dto;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class HisRecipeDetailDTO {
    private String recipeDeatilCode;
    private String drugCode;
    private String drugName;
    private BigDecimal price;
    //药品发药金额
    private Double amount;
    private BigDecimal totalPrice;
    //药品规格
    private String drModel;
    //频次名称 pcmc
    private String usingRate;
    //用法名称 yfmc
    private String usePathWays;
    //机构的频次代码"
    private String usingRateCode;
    //机构的用药途径代码
    private String usePathwaysCode;
    //剂量单位 jldw
    private String useDoseUnit;
    //药品剂量 ypjl
    private String useDose;
    //药品剂量 特殊用法
    private String useDoseStr;
    //药品单位 ypdw
    private String unit;
    //用药天数 yyts
    private String days;

    private String remark;
    //商品名
    private String saleName;
    //药品规格
    private String drugSpec;
    //批准文号
    private String licenseNumber;
    //药品本位码
    private String standardCode;
    //药品生产厂家
    private String producer;
    //厂家编码
    private String producerCode;
    //开药总数
    private BigDecimal useTotalDose;
    //包装单位
    private String drugUnit;
    //用药频率说明
    private String usingRateText;
    //用药方式说明
    private String usePathwaysText;
    //药品使用备注，如:饭前，饭后
    private String memo;
    //是否可流转 0 不可在互联网流转 1 可以流转
    private Integer status;
    //用药天数
    private Integer useDays;
    //包装数量
    private Integer pack;
    //用药天数（小数类型的）
    private String useDaysB;
    /**
     * 药房编码
     */
    private String pharmacyCode;
    /**
     * 药房名称
     */
    private String pharmacyName;
    /**
     * 药房类型
     */
    private String pharmacyCategray;
    /**
     * 中药禁忌类型(1:超量 2:十八反 3:其它)
     */
    private Integer tcmContraindicationType;
    /**
     * 中药禁忌原因
     */
    private String tcmContraindicationCause;
    /**
     * 药品嘱托编码
     */
    private String drugEntrustCode;
    /**
     * 剂型
     */
    private String drugForm;

    @ItemProperty(alias = "1:药品，2:诊疗项目，3 保密药品")
    private Integer type;


}
