package com.ngari.recipe.dto;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 院内门诊返回结果
 *
 * @author zgy
 * @date 2022/1/10 17:19
 */
@Setter
@Getter
public class WriteDrugRecipeDTO implements Serializable {

    private static final long serialVersionUID = -1182841922613719429L;

    /**
     * 患者信息
     */
    private PatientDTO patient;
    /**
     * 请求的患者信息
     */
    private PatientDTO requestPatient;
    /**
     * 门诊信息
     */
    private ConsultDTO consult;

    @ItemProperty(alias = "类型 默认复诊 2 复诊  5  门诊")
    private Integer type;

    @ItemProperty(alias = "挂号科室编码")
    private String appointDepartCode;

    @ItemProperty(alias = "挂号科室名称")
    private String appointDepartName;

    //组装给前端返回的字段对象
    private WriteDrugRecipeBean writeDrugRecipeBean;
}
