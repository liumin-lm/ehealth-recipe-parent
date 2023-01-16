package recipe.vo.second;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @description： 端用处方信息
 * @author： whf
 * @date： 2021-11-08 17:22
 */
@Getter
@Setter
@ToString
public class RecipeVo implements Serializable {

    @ItemProperty(alias = "处方序号")
    private Integer recipeId;

    @ItemProperty(alias = "开方机构名称")
    private String organName;

    @ItemProperty(alias = "开方科室")
    private String depart;

    @ItemProperty(alias = "开方医生（医生Id）")
    private String doctor;

    @ItemProperty(alias = "机构疾病名称")
    private String organDiseaseName;

    @ItemProperty(alias = "签名时间")
    private Date signDate;

    @ItemProperty(alias = "处方金额")
    private BigDecimal totalMoney;

    @ItemProperty(alias = "处方父状态：0：默认 ， 1：待提交，2：待审核，3：待够药，4：待发药，5：配送中，6：待取药，7：已完成，8：已删除 ，9：已作废")
    private Integer processState;

    @ItemProperty(alias = "处方子状态")
    private Integer subState;

    @ItemProperty(alias = "处方父状态text")
    private String processStateText;

    @ItemProperty(alias = "主索引（患者编号）")
    private String mpiid;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    @ItemProperty(alias = "处方来源类型 1 平台处方 2 线下转线上的处方 3诊疗处方")
    private Integer recipeSourceType;

    @ItemProperty(alias = "开方机构")
    private Integer clinicOrgan;

    @ItemProperty(alias = "处方审核方式")
    private Integer reviewType;
}
