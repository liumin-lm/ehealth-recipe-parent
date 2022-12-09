package recipe.vo.second;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @description： his支付回调请求入参
 * @author： whf
 * @date： 2022-08-11 11:35
 */
@Getter
@Setter
public class RecipePayHISCallbackReq implements Serializable {
    private static final long serialVersionUID = 5769855552356359557L;

    @ItemProperty(alias = "成功 200 失败 -1")
    private String msgCode;

    @ItemProperty(alias = "平台业务id 订单id")
    private Integer orderId;

    @ItemProperty(alias = "处方his单号")
    private String recipeCode;

    @ItemProperty(alias = "his收据号")
    private String hisSettlementNo;

    @ItemProperty(alias = "交易流水号")
    private String tradeNo;

    @ItemProperty(alias = "商户订单号")
    private String outTradeNo;

    @ItemProperty(alias = "处方总金额")
    private BigDecimal preSettleTotalAmount;

    @ItemProperty(alias = "自费金额")
    private BigDecimal cashAmount;

    @ItemProperty(alias = "医保金额")
    private BigDecimal fundAmount;

    @ItemProperty(alias = "是否医保结算 1 是")
    private Integer isMedicalSettle;

    @ItemProperty(alias = "结算模式 1 不走结算 ")
    private Integer settleMode;

    @ItemProperty(alias = "订单类型 1表示医保 其他自费")
    private Integer orderType;
}
