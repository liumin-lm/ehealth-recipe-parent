package com.ngari.recipe.recipe.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkLoadTopDTO {
    private String dispensingApothecaryName;
    private Integer recipeCount;
    private BigDecimal totalMoney;
}
