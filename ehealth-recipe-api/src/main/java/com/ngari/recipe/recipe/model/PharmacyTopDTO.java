package com.ngari.recipe.recipe.model;

import lombok.Data;

@Data
public class PharmacyTopDTO {
    private String drugId;
    private String drugName;
    private String drugSpec;
    private String drugUnit;
    private Integer count;
    private Double drugCost;
    private Double countMoney;
    private String drugtype;
}
