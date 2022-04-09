package com.ngari.recipe.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 医保授权信息
 */
@Getter
@Setter
public class MedicalInsuranceAuthInfoVO implements Serializable {
    private static final long serialVersionUID = 4697924405135453386L;

    private String mpiId;
    private Integer organId;
    private String callUrl;
}
