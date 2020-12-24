package com.ngari.recipe.recipe.model;

import lombok.Data;

import java.util.Date;

@Data
public class DispendingPharmacyReportReqTo {
    private Integer organId;
    private Date startDate;
    private java.util.Date endDate;
    private String drugName;
    private String cardNo;
    private String patientName;
    private String billNumber;
    private String recipeId;
    private Integer orderStatus;
    private Integer depart;
    private String doctorName;
    private String dispensingApothecaryName;
    private Integer recipeType;
    private Integer start;
    private Integer limit;
}
