package recipe.vo.second;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;
import recipe.vo.PageVO;

import java.util.List;

/**
 * 自助机对接查询对象
 * @author fuzi
 */
@Setter
@Getter
public class AutomatonVO extends PageVO {
    @ItemProperty(alias = "开始时间 ")
    private String startTime;
    @ItemProperty(alias = "结束时间 ")
    private String endTime;
    @ItemProperty(alias = "机构id")
    private Integer organId;
    @ItemProperty(alias = "处方id")
    private Integer recipeId;
    @ItemProperty(alias = "支付标志")
    private Integer payFlag;
    @ItemProperty(alias = "是否医保 0自费 1医保")
    private Integer medicalFlag;
    @ItemProperty(alias = "终端类型 1 自助机 ext对象")
    private Integer terminalType;
    @ItemProperty(alias = "终端ID ext对象")
    private List<String> terminalIds;
    @ItemProperty(alias = "处方父状态：0：默认 ， 1：待提交，2：待审核，3：待下单，4：待发药，5：配送中，6：待取药，7：已完成，8：已删除 ，9：已作废")
    private List<Integer> processStateList;
    @ItemProperty(alias = "申请量处方父状态：0：默认 ， 1：待提交，2：待审核，3：待下单，4：待发药，5：配送中，6：待取药，7：已完成，8：已删除 ，9：已作废")
    private List<Integer> applyProcessStateList;
    @ItemProperty(alias = "完成量处方父状态：0：默认 ， 1：待提交，2：待审核，3：待下单，4：待发药，5：配送中，6：待取药，7：已完成，8：已删除 ，9：已作废")
    private List<Integer> finishProcessStateList;
}
