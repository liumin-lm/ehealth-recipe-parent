package com.ngari.recipe.recipeorder.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * created by shiyuping on 2020/8/19
 */
@Data
public class MedicalRespData implements Serializable {

    private static final long serialVersionUID = -4903261917236429857L;
    /**本年账户支付*/
    private BigDecimal currentAccountPayment;
    /**历年账户支付*/
    private BigDecimal accountPaymentHistory;
    /**本年账户余额*/
    private BigDecimal currentAccountBalance;
    /**历年账户余额*/
    private BigDecimal accountBalanceHistory;
    /**基金支付*/
    private BigDecimal fundPayment;
    /**起付标准累计*/
    private BigDecimal accumulativeMinimumPaymentStandard;
    /**自费 */
    private BigDecimal selfPayment;
    /**其中历年账户（自费*/
    private BigDecimal annualAccountsBySelfPayment;
    /**自理*/
    private BigDecimal selfCare;
    /**其中历年账户（自理）*/
    private BigDecimal annualAccountsBySelfCare;
    /**自负*/
    private BigDecimal selfFinancing;
    /**其中历年账户（自负）*/
    private BigDecimal annualAccountsBySelfFinancing;
    /**合计*/
    private BigDecimal total;
}
