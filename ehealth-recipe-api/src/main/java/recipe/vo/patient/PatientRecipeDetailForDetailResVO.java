package recipe.vo.patient;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @description： 患者端处方详情页药品信息出参
 * @author： whf
 * @date： 2023-03-16 9:28
 */
@Data
@Schema
public class PatientRecipeDetailForDetailResVO implements Serializable {
    private static final long serialVersionUID = -3405528445671900414L;

    @ItemProperty(alias = "药品商品名")
    private String saleName;

    @ItemProperty(alias="处方明细序号")
    private Integer recipeDetailId;

    @ItemProperty(alias="处方序号")
    private Integer recipeId;

    @ItemProperty(alias="药品序号")
    private Integer drugId;

    @ItemProperty(alias="机构唯一索引")
    private String organDrugCode;

    @ItemProperty(alias="机构药品编号")
    private String drugItemCode;

    @ItemProperty(alias="药物名称")
    private String drugName;

    @ItemProperty(alias="药物规格")
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

    @ItemProperty(alias = "机构的频次代码")
    private String organUsingRate;
    @ItemProperty(alias = "机构的用法代码")
    private String organUsePathways;

    @ItemProperty(alias = "用药频率说明")
    private String usingRateTextFromHis; //防止覆盖原有usingRateText

    @ItemProperty(alias = "用药方式说明")
    private String usePathwaysTextFromHis;//防止覆盖原有usePathwaysText

    @ItemProperty(alias = "药物使用总数量")
    private Double useTotalDose;

    @ItemProperty(alias = "药物发放数量")
    private Double sendNumber;

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

    @ItemProperty(alias = "皮试药品标识： 0-非皮试药品， 1-皮试药品且需要皮试，2-皮试药品免试")
    private Integer skinTestFlag;

    @ItemProperty(alias = "腹透液  空0否  1是  ")
    private Integer peritonealDialysisFluidType;

    /**
     * 类型：1:药品，2:诊疗项目，3 保密药品....
     */
    private Integer type;

    @ItemProperty(alias = "中药禁忌类型(1:超量 2:十八反 3:其它)")
    private Integer tcmContraindicationType;

    @ItemProperty(alias = "中药禁忌原因")
    private String tcmContraindicationCause;

    @ItemProperty(alias = "单个药品医保类型 医保审批类型 0自费 1医保（默认0） 前端控制传入")
    private Integer drugMedicalFlag;


    /******************************** 以下数据 需要代码判断 ****************************/
    @ItemProperty(alias = "医保限定药标识 0 否 1 是")
    private Integer medicalInsuranceDrugFlag;

    /******************************** 以下数据 来源 organdurglist ****************************/
    @ItemProperty(alias = "是否靶向药  0否  1是 ")
    private Integer targetedDrugType;

    @ItemProperty(alias = "是否抗肿瘤药物  0否  1是 ")
    private Integer antiTumorDrugFlag;

    @ItemProperty(alias = "抗菌素药物等级 0：非抗菌素药物 1：1级 2：2级 3：3级 ")
    private Integer antibioticsDrugLevel;

    @ItemProperty(alias = "是否国家标准药品 0 否 1 是")
    private Integer nationalStandardDrugFlag;
}
