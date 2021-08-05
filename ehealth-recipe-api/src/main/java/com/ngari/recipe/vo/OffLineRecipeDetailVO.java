package com.ngari.recipe.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ngari.his.recipe.mode.RecipeDetailTO;
import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class OffLineRecipeDetailVO implements Serializable {
    private static final long serialVersionUID = -1686020050602722867L;
    @ItemProperty(alias = "患者姓名")
    private String patientName;

    @ItemProperty(alias = "病人性别")
    @Dictionary(id = "eh.base.dictionary.Gender")
    private String patientSex;

    @ItemProperty(alias = "患者年龄")
    private Integer age;

    @ItemProperty(alias = "是否儿科处方")
    private Boolean childRecipeFlag; //无

    @ItemProperty(alias = "监护人姓名")
    private String guardianName;

    @ItemProperty(alias = "医保类型编码")
    private Integer medicalType;
    @ItemProperty(alias = "医保类型")
    private String medicalTypeText;

    @ItemProperty(alias = "机构名称")
    private String organName;

    @ItemProperty(alias = "科室名称")
    private String departName;

    @ItemProperty(alias = "开具时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date createDate;

    @ItemProperty(alias = "诊断")
    private String organDiseaseName;

    @ItemProperty(alias = "处方类型 1西药 2中成药 3中药 4膏方")
    @Dictionary(id = "eh.cdr.dictionary.RecipeType")
    private Integer recipeType;

    @ItemProperty(alias = "是否为长处方 1是")
    private String isLongRecipe; //1

    @ItemProperty(alias = "是否加急处方 1是 0否")
    private Integer canUrgentAuditRecipe; //1

    @ItemProperty(alias = "药品信息")
    private List<RecipeDetailTO> recipeDetails;

    @ItemProperty(alias = "参考价格")
    private BigDecimal totalPrice;

    @ItemProperty(alias = "医生姓名")
    private String doctorName;

}