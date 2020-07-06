package com.ngari.recipe.recipereportform.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class RecivedDispatchedBalanceResponse implements Serializable{
    private static final long serialVersionUID = -2710572383200102535L;

    private Long total; //总计

    private Integer organId;

    private String enterpriseName; //药企名称

    private BigDecimal lastBalance; //上期结存

    private BigDecimal thisRecived; //本期收入

    private BigDecimal thisDispatched; //本期发出

    private BigDecimal thisBalance; //本期结存

}
