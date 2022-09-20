package recipe.vo.second;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author fuzi
 */
@Setter
@Getter
public class AutomatonResultVO{

    @ItemProperty(alias = "处方id")
    private Integer recipeId;
    @ItemProperty(alias = "支付标志")
    private Integer payFlag;
    @ItemProperty(alias = "是否医保 0自费 1医保")
    private Integer medicalFlag;
    @ItemProperty(alias = "终端ID ext对象")
    private String terminalId;
    @ItemProperty(alias = "处方父状态：0：默认 ， 1：待提交，2：待审核，3：待下单，4：待发药，5：配送中，6：待取药，7：已完成，8：已删除 ，9：已作废")
    private Integer processState;

    @ItemProperty(alias = "开方医生（医生Id）")
    private Integer doctor;
    @ItemProperty(alias = "医生姓名")
    private String doctorName;
    @ItemProperty(alias = "医生手机号")
    private String doctorMobile;


    @ItemProperty(alias = "主索引（患者编号）")
    private String mpiid;
    @ItemProperty(alias = "患者姓名")
    private String patientName;
    @ItemProperty(alias = "患者手机号")
    private String patientMobile;

    @ItemProperty(alias = "机构id")
    private Integer clinicOrgan;
    @ItemProperty(alias = "开方机构名称")
    private String organName;
    @ItemProperty(alias = "处方类型 1 西药 2 中成药 3 中药 4膏方")
    @Dictionary(id = "eh.cdr.dictionary.RecipeType")
    private Integer recipeType;
    @ItemProperty(alias = "药企序号")
    private Integer enterpriseId;
    @ItemProperty(alias = "药企名称")
    private String enterpriseName;
    @ItemProperty(alias = "支付日期")
    private Date payDate;
    @ItemProperty(alias = "处方金额")
    private BigDecimal totalMoney;
}
