package com.ngari.recipe.recipereportform.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class EnterpriseRecipeMonthSummaryResponse implements Serializable{

    private static final long serialVersionUID = 1971446018605579905L;

    private Long total; //总计

    private Integer organId; //机构id

    private String organName; //机构名称

    private String enterpriseName; //药企名称

    private Integer totalOrderNum; //总订单数

    private BigDecimal totalFee; //总费用

    private BigDecimal drugFee; //药费

    private BigDecimal deliveryFee; //配送费

    private BigDecimal ngariRecivedFee; //平台分润

    private BigDecimal organRecivedDiffFee; //医院实收应付差额

    /**
     * 药企序号
     */
    private Integer enterpriseId;
}
