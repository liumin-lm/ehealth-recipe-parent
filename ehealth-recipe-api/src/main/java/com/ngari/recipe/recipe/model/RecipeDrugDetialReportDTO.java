package com.ngari.recipe.recipe.model;

import lombok.Data;

@Data
public class RecipeDrugDetialReportDTO {
    private String billNumber;
    private Integer recipeId;
    private Integer depart;
    private String sendDate;
    private String patientName;
    private Integer status;
    private String sendApothecaryName;
    private String dispensingApothecaryName;
    private String dispensingWindow;
    private String doctorName;
    private Double totalMoney;
    private Integer recipeType;
    private String createDate;
    private String payTime;
}
