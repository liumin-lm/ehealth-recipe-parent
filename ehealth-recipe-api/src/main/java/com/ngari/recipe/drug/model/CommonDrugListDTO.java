package com.ngari.recipe.drug.model;

import lombok.Data;

/**
 * created by shiyuping on 2020/8/5
 */
@Data
public class CommonDrugListDTO implements java.io.Serializable{
    private static final long serialVersionUID = -393896513562456855L;
    private Integer doctor;
    private Integer organId;
    private Integer drugType;
    private Integer pharmacyId;

    public CommonDrugListDTO() {}

    public CommonDrugListDTO(Integer doctor, Integer organId, Integer drugType) {
        this.doctor = doctor;
        this.organId = organId;
        this.drugType = drugType;
    }
}
