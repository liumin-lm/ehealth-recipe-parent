package recipe.vo.base;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单基础的字段定义
 *
 * @author yinsheng
 */
@Getter
@Setter
public class BaseOrderVO implements Serializable {
    private static final long serialVersionUID = -7456227851943144105L;
    @ItemProperty(alias = "订单ID")
    private Integer orderId;

    @ItemProperty(alias = "订单编号")
    private String orderCode;

    @ItemProperty(alias = "订单支付标志 0未支付，1已支付")
    private Integer payFlag;

    @ItemProperty(alias = "挂号费")
    private BigDecimal registerFee;

    @ItemProperty(alias = "配送费")
    private BigDecimal expressFee;

    @ItemProperty(alias = "处方总费用")
    private BigDecimal recipeFee;

    @ItemProperty(alias = "订单总费用")
    private BigDecimal totalFee;

    @ItemProperty(alias = "实际支付费用")
    private Double actualPrice;

    @ItemProperty(alias = "配送费支付 0 未支付  1 已支付")
    private Integer expressFeePayFlag;

    @ItemProperty(alias = "交易流水号")
    private String tradeNo;

    @ItemProperty(alias = "商户订单号")
    private String outTradeNo;

    @ItemProperty(alias = "支付时间")
    private Date payTime;

    @ItemProperty(alias = "处方费用支付方式 1 线上支付 2 线下支付")
    private Integer payMode;
}
