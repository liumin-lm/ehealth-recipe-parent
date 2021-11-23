package com.ngari.recipe.dto;

import com.ngari.recipe.entity.PharmacyTcm;
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

    @ItemProperty(alias = "药房列表")
    private List<PharmacyTcm> pharmacyTcms;
}
