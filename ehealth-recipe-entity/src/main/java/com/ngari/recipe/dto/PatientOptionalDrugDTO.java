package com.ngari.recipe.dto;

import com.ngari.recipe.entity.PharmacyTcm;
import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * @description： 患者自选药品出参
 * @author： whf
 * @date： 2021-11-22 18:28
 */
@Setter
@Getter
@ToString
public class PatientOptionalDrugDTO implements Serializable {

    @ItemProperty(alias = "药品序号")
    private Integer drugId;

    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "就诊序号(对应来源的业务id)")
    private Integer clinicId;

    @ItemProperty(alias = "机构药品编号")
    private String organDrugCode;

    @ItemProperty(alias = "药物名称")
    private String drugName;

    @ItemProperty(alias = "药物规格")
    private String drugSpec;

    @ItemProperty(alias = "药物单位")
    private String drugUnit;

    @ItemProperty(alias = "患者指定的药品数量")
    private Integer patientDrugNum;

    @ItemProperty(alias = "已开方药品数量")
    private Integer openDrugNum;

    @ItemProperty(alias = "生产厂家")
    private String producer;

    @ItemProperty(alias = " 药品类型 1西药  2中成药 3中草药 4膏方")
    private Integer drugType;

    @ItemProperty(alias = "药物使用频率代码")
    @Dictionary(id = "eh.cdr.dictionary.UsingRate")
    private String usingRate;

    @ItemProperty(alias = "使用频率id")
    @Dictionary(id = "eh.cdr.dictionary.NewUsingRate")
    private String usingRateId;
    @ItemProperty(alias = "用药频次英文名称")
    private String usingRateEnglishNames;

    @ItemProperty(alias = "药物使用途径代码")
    @Dictionary(id = "eh.cdr.dictionary.UsePathways")
    private String usePathways;

    @ItemProperty(alias = "用药途径id")
    @Dictionary(id = "eh.cdr.dictionary.NewUsePathways")
    private String usePathwaysId;
    @ItemProperty(alias = "用药途径英文名称")
    private String usePathEnglishNames;

    @ItemProperty(alias = "剂量单位")
    private String useDoseUnit;

    @ItemProperty(alias = "剂型")
    private String drugForm;

    @ItemProperty(alias = "药品包装数量")
    private Integer pack;

    @ItemProperty(alias = "药品状态")
    private Integer status;

    @ItemProperty(alias = "前端展示的药品拼接名")
    private String drugDisplaySplicedName;

    @ItemProperty(alias = "药房列表")
    private List<PharmacyTcm> pharmacyTcms;
}
