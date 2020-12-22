package com.ngari.recipe.recipe.model;

import lombok.Data;

@Data
public class PharmacyMonthlyReportDTO {
    private Integer depart;
    private Double totalMoney;
    private Integer recipeCount;
    private Double avgMoney;
    private String departName;
}
