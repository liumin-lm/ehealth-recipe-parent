package recipe.vo.greenroom;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * @description： 退款信息 vo
 * @author： whf
 * @date： 2022-05-09 11:10
 */
@Getter
@Setter
public class RecipeRefundVO implements Serializable {
    @ItemProperty(alias = "主键ID")
    private Integer id;

    @ItemProperty(alias = "处方序号")
    private Integer busId;

    @ItemProperty(alias = "医院Id")
    private Integer  organId;

    @ItemProperty(alias = "患者Id")
    private String mpiid;

    @ItemProperty(alias = "患者Id")
    private String patientName;

    @ItemProperty(alias = "审核医生id")
    private Integer doctorId;

    @ItemProperty(alias = "支付流水号")
    private String tradeNo;

    @ItemProperty(alias = "订单金额")
    private Double price;

    @ItemProperty(alias = "退费申请序号")
    private String applyNo;

    @ItemProperty(alias = "当前节点")
    private Integer node;

    @ItemProperty(alias = "状态")
    private Integer status;

    @ItemProperty(alias = "申请审核理由")
    private String reason;

    @ItemProperty(alias = "审核时间")
    private Date checkTime;

    @ItemProperty(alias = "审核时间")
    private Date applyTime;

    @ItemProperty(alias = "前一节点")
    private Integer beforeNode;

    @ItemProperty(alias = "备注")
    private String memo;

    @ItemProperty(alias = "预留（后面要临时存扩展字段可以用键值对存）")
    private String expand;
}
