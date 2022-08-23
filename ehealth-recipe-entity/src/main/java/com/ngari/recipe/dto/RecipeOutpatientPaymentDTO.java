package com.ngari.recipe.dto;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @description： 门诊缴费支付回调入参
 * @author： whf
 * @date： 2022-08-18 14:13
 */
@Getter
@Setter
public class RecipeOutpatientPaymentDTO implements Serializable {
    private static final long serialVersionUID = -6404621147270133670L;

    @ItemProperty(alias = "处方his单号")
    private List<String> recipeCodes;

    @ItemProperty(alias = "处方his单号")
    private Integer organId;

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

    @ItemProperty(alias = "支付时间 ")
    private Date payTime;

    @ItemProperty(alias = "支付平台分配的机构id")
    private String payOrganId;
}
