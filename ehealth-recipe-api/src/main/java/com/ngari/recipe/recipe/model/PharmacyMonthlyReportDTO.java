package com.ngari.recipe.recipe.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PharmacyMonthlyReportDTO {
    private Integer depart;
    private BigDecimal totalMoney;
    private Integer recipeCount;
    private BigDecimal avgMoney;
    private String departName;
}
