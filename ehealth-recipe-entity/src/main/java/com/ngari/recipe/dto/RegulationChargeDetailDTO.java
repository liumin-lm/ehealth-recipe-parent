package com.ngari.recipe.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
/**
 * @author fuzi
 */
@Setter
@Getter
public class RegulationChargeDetailDTO implements Serializable {
    private static final long serialVersionUID = -8561990629250175795L;
    private Integer organID;
        private String organName;
        private String recipeDetailID;
        private Integer payFlag;
        private String clinicID;
        private String recipeID;
        private Integer recipeType;
        private String drugUnit;
        private BigDecimal actualSalePrice;
        private BigDecimal useTotalDose;
        private Integer status;
        private String tradeNo;
        private String medicalDrugCode;
        private BigDecimal salePrice;
    }
