package com.ngari.recipe.recipereportform.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class RecipeMonthAccountCheckResponse implements Serializable{
    private static final long serialVersionUID = 8599154333590559939L;

    private Long total; //总计

    private Integer organId; //机构id

    private String organName; //机构名称

    private Integer totalOrderNum; //总订单数

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


}
