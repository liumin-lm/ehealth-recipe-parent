package com.ngari.recipe.vo;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * @Description
 * @Author yzl
 * @Date 2022-08-16
 */
@Data
@Schema
public class FastRecipeVO implements Serializable {

    private static final long serialVersionUID = -4078142463611099079L;

    @ItemProperty(alias = "药方Id, 主键")
    private Integer id;

    @ItemProperty(alias = "排序序号")
    private Integer orderNum;

    @ItemProperty(alias = "药方说明")
    private String introduce;

    @ItemProperty(alias = "药方名称")
    private String title;

    @ItemProperty(alias = "药方图片")
    private String backgroundImg;

    @ItemProperty(alias = "处方类型")
    @Dictionary(id = "eh.cdr.dictionary.RecipeType")
    private Integer recipeType;

    @ItemProperty(alias = "销售数量上限")
    private Integer maxNum;

    @ItemProperty(alias = "销售数量下限")
    private Integer minNum;

    @ItemProperty(alias = "0-删除，1-上架，2-下架")
    private Integer status;

    @ItemProperty(alias = "最后需支付费用")
    private BigDecimal actualPrice;

    @ItemProperty(alias = "开方机构")
    private Integer clinicOrgan;

    @ItemProperty(alias = "开方机构名称")
    private String organName;

    @ItemProperty(alias = "剂数")
    private Integer copyNum;

    @ItemProperty(alias = "处方金额")
    private BigDecimal totalMoney;

    @ItemProperty(alias = "发药方式")
    private Integer giveMode;

    @ItemProperty(alias = "来源标志")
    private Integer fromFlag;

    @ItemProperty(alias = "诊断备注")
    private String memo;

    @ItemProperty(alias = "制法")
    private String makeMethodId;

    @ItemProperty(alias = "制法text")
    private String makeMethodText;

    @ItemProperty(alias = "每付取汁")
    private String juice;

    @ItemProperty(alias = "每付取汁单位")
    private String juiceUnit;

    @ItemProperty(alias = "次量")
    private String minor;

    @ItemProperty(alias = "次量单位")
    private String minorUnit;

    @ItemProperty(alias = "中医症候编码")
    private String symptomId;

    @ItemProperty(alias = "中医症候名称")
    private String symptomName;

    @ItemProperty(alias = "煎法")
    private String decoctionId;

    @ItemProperty(alias = "煎法text")
    private String decoctionText;

    @ItemProperty(alias = "煎法单价")
    private Double decoctionPrice;

    @ItemProperty(alias = "线下处方名称")
    private String offlineRecipeName;

    @ItemProperty(alias = "电子病历文本")
    private String docText;

    @ItemProperty(alias = "是否需要问卷，0不需要，1需要")
    private Integer needQuestionnaire;

    @ItemProperty(alias = "问卷链接")
    private String questionnaireUrl;

    @ItemProperty(alias = "药方详情，药品信息")
    private List<FastRecipeDetailVO> fastRecipeDetailList;

    @ItemProperty(alias = "医生选择是否代煎（ 0:否 1：是）")
    private String doctorIsDecoction;

    @ItemProperty(alias = "代煎前端展示 0 不展示 1 展示")
    private Integer decoctionExhibitionFlag;

    @ItemProperty(alias = "每贴次数")
    private String everyTcmNumFre;

    @ItemProperty(alias = "中药嘱托")
    private String recipeMemo;

    @ItemProperty(alias = "服用要求")
    private String requirementsForTakingId;

    @ItemProperty(alias = "服用要求code")
    private String requirementsForTakingCode;

    @ItemProperty(alias = "服用要求text")
    private String requirementsForTakingText;

    @ItemProperty(alias = "处方剂型类型 1 饮片方 2 颗粒方")
    @Dictionary(id = "eh.cdr.dictionary.RecipeDrugForm")
    private Integer recipeDrugForm;

    @ItemProperty(alias = "单复方表示：0无状态，1单方，2复方")
    private Integer singleOrCompoundRecipe;

    @ItemProperty(alias = "代煎帖数")
    private Integer decoctionNum;

    @ItemProperty(alias = "是否保密， 1:保密，2:非保密")
    private Integer secrecyFlag;

    @ItemProperty(alias = "处方支持的购药方式,逗号分隔")
    private String recipeSupportGiveMode;

    @ItemProperty(alias = "配送药企代码")
    private String deliveryCode;

    @ItemProperty(alias = "处方指定药企类型: 1医院, 2药企, 默认0")
    private Integer appointEnterpriseType;

    @ItemProperty(alias = "药方库存")
    private Integer stockNum;
}
