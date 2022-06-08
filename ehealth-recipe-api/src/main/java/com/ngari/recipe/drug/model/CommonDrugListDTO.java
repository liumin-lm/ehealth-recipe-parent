package com.ngari.recipe.drug.model;

import lombok.Data;

/**
 * created by shiyuping on 2020/8/5
 *
 * @author fuzi
 */
@Data
public class CommonDrugListDTO implements java.io.Serializable {
    private static final long serialVersionUID = -393896513562456855L;
    /**
     * 医生id
     */
    private Integer doctor;
    /**
     * 机构id
     */
    private Integer organId;
    /**
     * 药品类型
     */
    private Integer drugType;
    /**
     * 药房id
     */
    private Integer pharmacyId;

    public CommonDrugListDTO() {
    }
    
    public CommonDrugListDTO(Integer doctor, Integer organId, Integer drugType) {
        this.doctor = doctor;
        this.organId = organId;
        this.drugType = drugType;
    }
}
