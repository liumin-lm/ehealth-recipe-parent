package recipe.vo.patient;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @description： 患者端 处方详情出参
 * @author： whf
 * @date： 2023-03-15 11:32
 */
@Data
@Schema
public class PatientRecipeDetailResVO implements Serializable {
    private static final long serialVersionUID = 6997161672673473142L;
    @ItemProperty(alias = "处方序号")
    private Integer recipeId;

    @ItemProperty(alias = "处方号码，处方回写")
    private String recipeCode;

    @ItemProperty(alias = "订单编号")
    private String orderCode;

    @ItemProperty(alias = "开处方来源 1问诊 2复诊(在线续方) 3网络门诊")
    private Integer bussSource;

    @ItemProperty(alias = "就诊序号(对应来源的业务id)")
    private Integer clinicId;

    @ItemProperty(alias = "主索引")
    private String mpiid;

    @ItemProperty(alias = "处方类型")
    @Dictionary(id = "eh.cdr.dictionary.RecipeType")
    private Integer recipeType;

    @ItemProperty(alias = "开方科室")
    private Integer depart;
    private String departName;

    @ItemProperty(alias = "开方医生（医生Id）")
    private Integer doctor;
    private String doctorName;

    @ItemProperty(alias = "机构疾病名称")
    private String organDiseaseName;

    @ItemProperty(alias = "处方父状态：0：默认 ， 1：待提交，2：待审核，3：待够药，4：待发药，5：配送中，6：待取药，7：已完成，8：已删除 ，9：已作废")
    @Dictionary(id = "eh.recipe.recipeState.process")
    private Integer processState;

    @ItemProperty(alias = "处方子状态")
    private Integer subState;

    @ItemProperty(alias = "处方子状态文本")
    private String subStateText;

    @ItemProperty(alias = "签名时间")
    private Date signDate;

    @ItemProperty(alias = "处方来源类型 1 平台处方 2 线下转线上的处方 3诊疗处方 4常用方")
    private Integer recipeSourceType;

    @ItemProperty(alias = "是否靶向药  0否  1是 ")
    private Integer targetedDrugType;

    @ItemProperty(alias = "线下处方/常用方/协定方名称")
    private String offlineRecipeName;

    @ItemProperty(alias = "是否医保 0自费 1医保")
    private Integer medicalFlag;

    @ItemProperty(alias = "处方金额")
    private BigDecimal totalMoney;

    @ItemProperty(alias = "开方机构")
    @Dictionary(id = "eh.base.dictionary.Organ")
    private Integer clinicOrgan;

    @ItemProperty(alias = "开方机构名称")
    private String organName;

    @ItemProperty(alias = "西药用药嘱托")
    private String memo;

    @ItemProperty(alias = "中药用药嘱托")
    private String recipeMemo;

    @ItemProperty(alias = "门诊号")
    private String patientID;

    @ItemProperty(alias = "剂数")
    private Integer copyNum;

    @ItemProperty(alias = "支付标志")
    private Integer payFlag;

    @ItemProperty(alias = "代煎帖数")
    private Integer decoctionNum;

    @ItemProperty(alias = "发药方式")
    private Integer giveMode;

    /******************************** 以下数据来源 recipeExt ****************************/

    @ItemProperty(alias = "是否长处方")
    private String isLongRecipe;

    @ItemProperty(alias = "可开长处方按钮状态、长处方开药天数、非长处方开药天数等")
    private String recipeJsonConfig;

    @ItemProperty(alias = "病历索引Id")
    private Integer docIndexId;

    @ItemProperty(alias = "处方标识 0:普通处方 1:儿童处方")
    private Integer recipeFlag;

    @ItemProperty(alias = "是否是加急审核处方 0否 1是")
    private Integer canUrgentAuditRecipe;

    @ItemProperty(alias = "病种代码")
    private String chronicDiseaseCode;

    @ItemProperty(alias = "病种名称")
    private String chronicDiseaseName;

    @ItemProperty(alias = "挂号序号")
    private String registerID;

    @ItemProperty(alias = "制法")
    private String makeMethodId;
    @ItemProperty(alias = "制法text")
    private String makeMethodText;
    @ItemProperty(alias = "每贴次数")
    private String everyTcmNumFre;
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
    @ItemProperty(alias = "服用要求")
    private String requirementsForTakingId;
    @ItemProperty(alias = "服用要求code")
    private String requirementsForTakingCode;
    @ItemProperty(alias = "服用要求text")
    private String requirementsForTakingText;

    @ItemProperty(alias = "单复方表示0:无状态，1单方，2复方")
    private Integer singleOrCompoundRecipe;

    @ItemProperty(alias = "处方剂型类型 1 饮片方 2 颗粒方")
    @Dictionary(id = "eh.cdr.dictionary.RecipeDrugForm")
    private Integer recipeDrugForm;

    @ItemProperty(alias = "医生选择是否代煎（ 0:否 1：是）")
    private String doctorIsDecoction;

    @ItemProperty(alias = "代煎前端展示 0 不展示 1 展示")
    private Integer decoctionExhibitionFlag;

    @ItemProperty(alias = "用药医嘱(药师审方)")
    private String drugEntrustment;

    /******************************** 以下数据来源 recipedetail ****************************/
    @ItemProperty(alias = "药房id主键")
    private Integer pharmacyId;
    @ItemProperty(alias = "药房名称")
    private String pharmacyName;
    @ItemProperty(alias = "药品详情")
    private List<PatientRecipeDetailForDetailResVO> recipeDetail;

    /******************************** 以下数据 需要代码判断 ****************************/
    @ItemProperty(alias = "处方业务查询来源 1 线上  2 线下 3 院内门诊")
    private Integer recipeBusType;

    @ItemProperty(alias = "隐方")
    private Boolean isHiddenRecipeDetail = false;

    @ItemProperty(alias = "是否保密方 0 否 1 是")
    private Integer secrecyRecipe;

    /**
     * 复诊 ex 表 orderNumStartTime 如果没有值就取复诊 主表的  PaymentDate
     */
    @ItemProperty(alias = "就诊时间")
    private Date visitTime;

    @ItemProperty(alias = "是否可以下载处方笺")
    private Boolean isDownload;

}
