package com.ngari.recipe.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * @author fuzi
 */
@Getter
@Setter
public class HisRecipeInfoDTO {
    private String recipeCode;
    private String disease;
    private String diseaseName;
    private String recipeType;
    private String signTime;
    private String departCode;
    private String departName;
    private String doctorCode;
    private String doctorName;
    private Integer checkStatus;
    private Integer queryStatus;
    private Integer copyNum;
    private String recipeMemo;
    /**
     * 就诊序列号
     */
    private String serialNumber;
}
