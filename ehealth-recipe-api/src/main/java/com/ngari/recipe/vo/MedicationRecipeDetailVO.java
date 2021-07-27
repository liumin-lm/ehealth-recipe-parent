package com.ngari.recipe.vo;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 药品详情
 * @author ys
 */
@Data
public class MedicationRecipeDetailVO implements Serializable{
    @ItemProperty(alias="药品序号")
    private Integer drugId;

    @ItemProperty(alias="机构药品编号")
    private String organDrugCode;

    @ItemProperty(alias="药物名称")
    private String drugName;

    @ItemProperty(alias="处方明细序号")
    private Integer recipeDetailId;

    @ItemProperty(alias="用药频率说明")
    private String usingRateTextFromHis;

    @ItemProperty(alias="用药方式说明")
    private String usePathwaysTextFromHis;

    @ItemProperty(alias="药物使用次剂量")
    private Double useDose;

    @ItemProperty(alias="药物使用途径代码")
    private String usePathways;

    @ItemProperty(alias="药物使用频率代码")
    private String usingRate;

    @ItemProperty(alias="药物使用规格单位或者最小单位")
    private String useDoseUnit;


}
