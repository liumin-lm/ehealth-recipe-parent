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
     * 收费项
     */
    private ChargeItemDTO chargeItemDTO;
    /**
     * 发药方式
     */
    private String giveModeText;
    /**
     * 线下处方温馨提示
     */
    private String showText;
    /**
     * 处方所属类型 1 线上 2 线下
     */
    private String recipeBusType;

}
