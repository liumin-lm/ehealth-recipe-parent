package com.ngari.recipe.commonrecipe.model;

import com.ngari.recipe.drug.model.UseDoseAndUnitRelationBean;
import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Created by  on 2017/5/23.
 * 常用方药品
 *
 * @author jiangtingfeng
 */
@Schema
@Getter
@Setter
public class CommonRecipeDrugDTO implements java.io.Serializable {

    private static final long serialVersionUID = -4535607360492071383L;

    @ItemProperty(alias = "常用方编码-医院唯一主键字段")
    private String commonRecipeCode;

    @ItemProperty(alias = "药房编码")
    private String pharmacyCode;

    @ItemProperty(alias = "药房id主键")
    private Integer pharmacyId;

    @ItemProperty(alias = "药房名称")
    private String pharmacyName;

    @ItemProperty(alias = "药房类型：中药,西药,中成药,膏方")
    private List<String> pharmacyCategray;

    @ItemProperty(alias = "自增id")
    private Integer id;

    @ItemProperty(alias = "药品状态")
    private Integer drugStatus;

    @ItemProperty(alias = "常用方Id")
    private Integer commonRecipeId;

    @ItemProperty(alias = "药品ID")
    private Integer drugId;

    @ItemProperty(alias = "机构药品编码")
    private String organDrugCode;

    @ItemProperty(alias = "药物名称")
    private String drugName;

    @ItemProperty(alias = "商品名")
    private String saleName;

    @ItemProperty(alias = "药物单位")
    private String drugUnit;

    @ItemProperty(alias = "药物规格")
    private String drugSpec;

    @ItemProperty(alias = "药物使用总数量")
    private Double useTotalDose;

    @ItemProperty(alias = "药物使用次剂量")
    private Double useDose;

    @ItemProperty(alias="药物使用次剂量--中文标识-适量")
    private String useDoseStr;

    @ItemProperty(alias = "默认每次剂量")
    private Double defaultUseDose;

    @ItemProperty(alias = "销售价格")
    private BigDecimal salePrice;

    @ItemProperty(alias = "销售单价")
    private Double price1;

    @ItemProperty(alias = "总药物金额")
    private BigDecimal drugCost;

    @ItemProperty(alias = "备注信息")
    /**药品嘱托test*/
    private String memo;

    @ItemProperty(alias = "药品嘱托编码")
    private String drugEntrustCode ;

    @ItemProperty(alias = "药品嘱托Id")
    private String drugEntrustId ;


    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

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

    @ItemProperty(alias = "药物使用天数")
    private Integer useDays;

    @ItemProperty(alias = "剂量单位")
    private String useDoseUnit;

    @ItemProperty(alias = "平台商品名")
    private String platformSaleName;

    @ItemProperty(alias = "剂型")
    private String drugForm;

    @ItemProperty(alias = "医生端选择的每次剂量和单位绑定关系")
    private List<UseDoseAndUnitRelationBean> useDoseAndUnitRelation;

    @ItemProperty(alias = "药品包装数量")
    private Integer pack;

    @ItemProperty(alias = "机构药品药房id")
    private String organPharmacyId;

    @ItemProperty(alias = "中药禁忌类型(1:超量 2:十八反 3:其它)")
    private Integer tcmContraindicationType;

    @ItemProperty(alias = "中药禁忌原因")
    private String tcmContraindicationCause;

    @ItemProperty(alias = "前端展示的药品拼接名")
    private String drugDisplaySplicedName;

    @ItemProperty(alias = "前端展示的商品拼接名")
    private String drugDisplaySplicedSaleName;

    @ItemProperty(alias = "药品超量编码")
    private String superScalarCode;

    @ItemProperty(alias = "药品超量名称")
    private String superScalarName;
}
