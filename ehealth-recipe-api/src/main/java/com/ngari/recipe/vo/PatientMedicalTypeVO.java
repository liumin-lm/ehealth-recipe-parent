package com.ngari.recipe.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2021\8\8 0008 19:12
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatientMedicalTypeVO implements Serializable{
    private static final long serialVersionUID = 7889149001189890072L;

    /**医保代码ybdm*/
    private String medicalType;
    /**医保说明ybsm*/
    private String medicalTypeText;
}
