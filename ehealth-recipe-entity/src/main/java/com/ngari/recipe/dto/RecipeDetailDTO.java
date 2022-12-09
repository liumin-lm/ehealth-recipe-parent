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
    private Integer drugId;

    @ItemProperty(alias = "机构药品编号")
    private String organDrugCode;

    @ItemProperty(alias = "处方药品详情类型")
    private Integer drugType;

    @ItemProperty(alias = "药房id主键")
    private Integer pharmacyId;

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
    @Deprecated
    private String usePathWays;

    @ItemProperty(alias = "用法名称")
    private String usePathways;

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

    @ItemProperty(alias = "皮试药品标识： 0-非皮试药品， 1-皮试药品且需要皮试，2-皮试药品免试")
    private Integer skinTestFlag;

    @ItemProperty(alias = "返回his药品状态 0:正常，1大病无权限，2靶向药无权限，3抗肿瘤药物无权限 " +
            "4抗菌素药物无权限 5“精”“麻”“毒”“放”类药品，禁止在互联网医院上开具，6“特殊使用级抗生素”类药品，禁止在互联网医院上开具")
    private Integer validateHisStatus;
    /**
     * 返回his药品状态 原因
     */
    private String validateHisStatusText;
    /**
     * 剩余天数
     */
    private Integer residueDay;


    public String getUsePathways() {
        return usePathways;
    }

    public void setUsePathways(String usePathways) {
        this.usePathways = usePathways;
    }
}
