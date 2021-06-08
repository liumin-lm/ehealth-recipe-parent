package recipe.dao.bean;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.FileToken;
import ctd.schema.annotation.ItemProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * @description： 患者端线上 处方列表查询 bean
 * @author： whf
 * @date： 2021-06-03 15:14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecipeListBean implements Serializable {

    /**
     * 处方单号
     */
    private Integer recipeId;
    /**
     * 订单编号
     */
    private String orderCode;
    /**
     * 处方状态
     */
    @Dictionary(id = "eh.cdr.dictionary.RecipeStatus")
    private Integer status;
    private String statusText;

    @ItemProperty(alias = "患者姓名")
    private String patientName;

    /**
     * 处方来源
     */
    private Integer fromFlag;

    @ItemProperty(alias = "处方号码，处方回写")
    private String recipeCode;

    @ItemProperty(alias = "医生姓名")
    private String doctorName;

    @ItemProperty(alias = "处方类型 1 西药 2 中成药")
    @Dictionary(id = "eh.cdr.dictionary.RecipeType")
    private Integer recipeType;

    @ItemProperty(alias = "诊断名称")
    private String organDiseaseName;

    @ItemProperty(alias = "开方机构")
    @Dictionary(id = "eh.base.dictionary.Organ")
    private Integer clinicOrgan;

    @ItemProperty(alias = "开方机构名称")
    private String organName;

    @ItemProperty(alias = "签名的处方PDF")
    private String signFile;

    @ItemProperty(alias = "药师签名的处方PDF")
    private String chemistSignFile;

    @ItemProperty(alias = "签名时间")
    private Date signDate;

    @ItemProperty(alias = "处方流转模式")
    private String recipeMode;

    @ItemProperty(alias = "处方单特殊来源标识：1省中，邵逸夫医保小程序;  2北京 默认null")
    private Integer recipeSource;

    @ItemProperty(alias = "主索引（患者编号）")
    private String mpiid;

    @ItemProperty(alias = "开方科室")
    @Dictionary(id = "eh.base.dictionary.Depart")
    private Integer depart;

    @ItemProperty(alias = "药企序号")
    private Integer enterpriseId;

    @ItemProperty(alias = "发药方式")
    private Integer giveMode;

    /*****************************************  以下来源 ext 表 ******************************************************/

    @ItemProperty(alias = "挂号序号")
    private String registerID;

    @ItemProperty(alias = "病种名称")
    private String chronicDiseaseName;

    /*****************************************  以下来源 order 表 ******************************************************/
    @ItemProperty(alias = "订单ID")
    private Integer orderId;

    @ItemProperty(alias = "排序使用")
    private Date time;

    @ItemProperty(alias = "订单状态")
    private Integer orderStatus;


}
