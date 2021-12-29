package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;
import lombok.Data;

import java.util.Date;

@Data
public class DispendingPharmacyReportReqTo {
    private Integer organId;
    private Date startDate;
    private java.util.Date endDate;
    private String drugName;
    @Desensitizations(type = DesensitizationsType.HEALTHCARD)
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
