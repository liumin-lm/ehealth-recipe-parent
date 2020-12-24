package com.ngari.recipe.recipe.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PharmacyTopDTO {
    private String drugId;
    private String drugName;
    private String drugSpec;
    private String drugUnit;
    private String count;
    private BigDecimal drugCost;
    private BigDecimal countMoney;
    private String drugtype;
}
