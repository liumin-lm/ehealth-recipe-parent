package com.ngari.recipe.recipe.model;

import lombok.Data;

@Data
public class RecipeDrugDetialReportDTO {
    private String billNumber;
    private Integer recipeId;
    private Integer depart;
    private String departName;
    private String sendDate;
    private String patientName;
    private String status;
    private String sendApothecaryName;
    private String dispensingApothecaryName;
    private String dispensingWindow;
    private String doctorName;
    private Double totalMoney;
    private String recipeType;
    private String createDate;
    private String payTime;
}
