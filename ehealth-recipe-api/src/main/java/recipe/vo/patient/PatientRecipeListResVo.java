package recipe.vo.patient;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @description： 患者端 处方列表出参
 * @author： whf
 * @date： 2023-03-01 9:42
 */
@Data
@Schema
public class PatientRecipeListResVo implements Serializable {
    private static final long serialVersionUID = -4426373463637935321L;

    @ItemProperty(alias = "处方序号")
    private Integer recipeId;

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

    @ItemProperty(alias = "签名时间")
    private Date signDate;

    @ItemProperty(alias = "处方来源类型 1 平台处方 2 线下转线上的处方 3诊疗处方 4常用方")
    private Integer recipeSourceType;

    @ItemProperty(alias = "处方号码，处方回写")
    private String recipeCode;

    @ItemProperty(alias = "是否靶向药  0否  1是 ")
    private Integer targetedDrugType;

    @ItemProperty(alias = "线下处方/常用方/协定方名称")
    private String offlineRecipeName;

    @ItemProperty(alias = "支付标志")
    private Integer payFlag;

    /******************************** 以下数据来源 recipeExt ****************************/
    @ItemProperty(alias = "大病类型")
    private String illnessType;

    @ItemProperty(alias = "大病类型名称")
    private String illnessName;

    @ItemProperty(alias = "挂号序号")
    private String registerID;


    /******************************** 以下数据 需要代码判断 ****************************/
    @ItemProperty(alias = "处方业务查询来源 1 线上  2 线下 3 院内门诊")
    private Integer recipeBusType;

    @ItemProperty(alias = "是否保密方 0 否 1 是")
    private Integer secrecyRecipe;

    @ItemProperty(alias = "腹透液  空0否  1是 ")
    private Integer peritonealDialysisFluidType;

    @ItemProperty(alias = "隐方")
    private Boolean isHiddenRecipeDetail = false;

    /******************************** 以下数据来源 recipeDetail ****************************/
    @ItemProperty(alias = "药品信息")
    private List<RecipeDetailForRecipeListResVo> recipeDetail;
}
