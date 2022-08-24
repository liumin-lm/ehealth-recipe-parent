package com.ngari.recipe.vo;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Setter
@Getter
@Schema
public class FastRecipeDetailVO implements Serializable {
    private static final long serialVersionUID = -1666706830573590374L;

    @ItemProperty(alias = "主键")
    private Integer id;

    @ItemProperty(alias = "药方Id")
    private Integer fastRecipeId;

    @ItemProperty(alias = "药品商品名")
    private String saleName;

    @ItemProperty(alias = "药品序号")
    private Integer drugId;

    @ItemProperty(alias = "机构唯一索引")
    private String organDrugCode;

    @ItemProperty(alias = "机构药品编号")
    private String drugItemCode;

    @ItemProperty(alias = "药物名称")
    private String drugName;

    @ItemProperty(alias = "药物规格")
    private String drugSpec;

    @ItemProperty(alias = "药品包装数量")
    private Integer pack;

    @ItemProperty(alias = "药物单位")
    private String drugUnit;

    @ItemProperty(alias = "药物使用次剂量")
    private Double useDose;

    @ItemProperty(alias = "默认每次剂量")
    private Double defaultUseDose;

    @ItemProperty(alias = "药物使用次剂量--中文标识-适量")
    private String useDoseStr;

    @ItemProperty(alias = "药物使用规格单位")
    private String useDoseUnit;

    @ItemProperty(alias = "药物剂量单位")
    private String dosageUnit;

    @ItemProperty(alias = "平台药物使用频率代码")
    @Dictionary(id = "eh.cdr.dictionary.UsingRate")
    private String usingRate;

    @ItemProperty(alias = "平台药物使用途径代码")
    @Dictionary(id = "eh.cdr.dictionary.UsePathways")
    private String usePathways;

    @ItemProperty(alias = "使用频率id")
    @Dictionary(id = "eh.cdr.dictionary.NewUsingRate")
    private String usingRateId;

    @ItemProperty(alias = "用药途径id")
    @Dictionary(id = "eh.cdr.dictionary.NewUsePathways")
    private String usePathwaysId;

    @ItemProperty(alias = "医院频次代码")
    private String organUsingRate;

    @ItemProperty(alias = "机构的用法代码")
    private String organUsePathways;

    @ItemProperty(alias = "用药频率说明, 防止覆盖原有usingRateText")
    private String usingRateTextFromHis;

    @ItemProperty(alias = "用药方式说明")
    private String usePathwaysTextFromHis;

    @ItemProperty(alias = "药物使用总数量")
    private Double useTotalDose;

    @ItemProperty(alias = "药物使用天数")
    private Integer useDays;

    @ItemProperty(alias = "药物金额 = useTotalDose * salePrice")
    private BigDecimal drugCost;

    @ItemProperty(alias = "药品嘱托Id")
    private String entrustmentId;

    @ItemProperty(alias = "药品嘱托信息")
    private String memo;

    @ItemProperty(alias = "药品嘱托编码")
    private String drugEntrustCode;

    @ItemProperty(alias = "药品有效期")
    private Date validDate;

    @ItemProperty(alias = "销售价格 = organDrug.salePrice")
    private BigDecimal salePrice;

    @ItemProperty(alias = "药品编码")
    private String drugCode;

    @ItemProperty(alias = "是否启用")
    private Integer status;

    @ItemProperty(alias = "剂型")
    private String drugForm;

    @ItemProperty(alias = "生产厂家")
    private String producer;

    @ItemProperty(alias = "生产厂家代码")
    private String producerCode;

    @ItemProperty(alias = "药物使用天数(小数类型)")
    private String useDaysB;

    @ItemProperty(alias = "处方药品详情类型")
    private Integer drugType;

    @ItemProperty(alias = "药品超量编码")
    private String superScalarCode;

    @ItemProperty(alias = "药品超量名称")
    private String superScalarName;

    @ItemProperty(alias = "医保药品编码")
    private String medicalDrugCode;

    @ItemProperty(alias = "药房id主键")
    private Integer pharmacyId;

    @ItemProperty(alias = "药房名称")
    private String pharmacyName;

    @ItemProperty(alias = "前端展示的药品拼接名")
    private String drugDisplaySplicedName;

    @ItemProperty(alias = "前端展示的商品拼接名")
    private String drugDisplaySplicedSaleName;

    @ItemProperty(alias = "单个药品医保类型 医保审批类型 0自费 1医保（默认0） 前端控制传入")
    private Integer drugMedicalFlag;

    @ItemProperty(alias = "类型：1:药品，2:诊疗项目，3....")
    private Integer type;

}
