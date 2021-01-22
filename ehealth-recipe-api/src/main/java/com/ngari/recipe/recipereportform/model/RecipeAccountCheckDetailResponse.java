package com.ngari.recipe.recipereportform.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class RecipeAccountCheckDetailResponse implements Serializable{
    private static final long serialVersionUID = 5501099787443188752L;

    private Long total; //总计

    private Integer recipeId; //处方单号

    private String patientName; //患者姓名

    private Date payDate; //支付时间

    private BigDecimal totalFee; //总费用

    private BigDecimal drugFee; //药费

    private BigDecimal registerFee; //挂号费

    private BigDecimal checkFee; //审方费

    private BigDecimal deliveryFee; //配送费

    private BigDecimal organActualRecivedFee; //医院实收费用

    private BigDecimal medicalInsurancePlanningFee; //医保统筹

    private BigDecimal organAccountRecivedFee; //医院账户实收

    private BigDecimal ngariAccountRecivedFee; //平台账户实收

    private BigDecimal organRecivedDiffFee; //医院实收应付差额

    private String tradeNo; //商户订单号

    private String mpiId; //用户mpiid

    private String recipeCode; //his处方单号
    /**
     * 订单退款标识 0未退费 1已退费
     */
    private Integer refundFlag;

    /**
     * 订单状态信息，当refundFlag=0时显示 未退费
     * refundFlag=1时显示 已经退费
     */
    private String refundMessage;

    /**
     * 药企序号
     */
    private Integer enterpriseId;
    /**
     * 药企名称
     */
    private String enterpriseName;

}
