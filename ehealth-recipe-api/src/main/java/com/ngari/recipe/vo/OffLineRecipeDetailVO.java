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

    @ItemProperty(alias = "患者出生日期")
    private Date patientBirthday;

    @ItemProperty(alias = "是否儿科处方")
    private Boolean childRecipeFlag;

    @ItemProperty(alias = "监护人姓名")
    private String guardianName;

    @ItemProperty(alias = "监护人年龄")
    private Integer guardianAge;

    @ItemProperty(alias = "监护人性别")
    private String guardianSex;

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

    private String recipeTypeText;

    @ItemProperty(alias = "是否为长处方 1是")
    private String isLongRecipe;

    @ItemProperty(alias = "是否加急处方 1是 0否")
    private Integer canUrgentAuditRecipe;

    @ItemProperty(alias = "药品信息")
    private List<RecipeDetailVO> recipeDetails;

    @ItemProperty(alias = "参考价格")
    private BigDecimal totalPrice;

    @ItemProperty(alias = "医生姓名")
    private String doctorName;

    @ItemProperty(alias = "处方嘱托")
    private String recipeMemo;

    @ItemProperty(alias = "中药用法")
    private String tcmUsePathways;

    @ItemProperty(alias = "中药使用频率")
    private String tcmUsingRate;

    @ItemProperty(alias = "贴数")
    private Integer tcmNum;

    @ItemProperty(alias = "煎法编码")
    private String decoctionCode;

    @ItemProperty(alias = "煎法")
    private String decoctionText;

    @ItemProperty(alias = "制法编码")
    private String makeMethodCode;

    @ItemProperty(alias = "制法")
    private String makeMethodText;

    @ItemProperty(alias = "每付取汁")
    private String juice;

    @ItemProperty(alias = "每付取汁单位")
    private String juiceUnit;

    @ItemProperty(alias = "次量")
    private String minor;

    @ItemProperty(alias = "次量单位")
    private String minorUnit;

    @ItemProperty(alias = "病种编码")
    private String chronicDiseaseCode;

    @ItemProperty(alias = "病种名称")
    private String chronicDiseaseName;

    @ItemProperty(alias = "开处方页面病种选择开关标识")
    private Integer recipeChooseChronicDisease;

    @ItemProperty(alias = "审核医生姓名")
    private String checkerName;
    //预留字段
    @ItemProperty(alias = "关联病例ID")
    private Integer docIndexId;
}