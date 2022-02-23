package com.ngari.recipe.dto;

import com.ngari.recipe.entity.RecipeTherapy;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author fuzi
 */
@Setter
@Getter
public class RecipeInfoDTO extends RecipeDTO implements Serializable {
    private static final long serialVersionUID = 4097986146206606609L;
    /**
     * 患者信息
     */
    private PatientDTO patientBean;
    /**
     * 签名信息
     */
    private ApothecaryDTO apothecary;
    /**
     * 处方诊疗信息
     */
    private RecipeTherapy recipeTherapy;
    /**
     * 机构信息
     */
    private OrganDTO organ;
    /**
     * 复诊时间
     */
    private String revisitTime;
    /**
     * 是否代煎
     */
    private Boolean generationisOfDecoction;

}
