package com.ngari.recipe.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 医保授权返回
 */
@Getter
@Setter
public class MedicalInsuranceAuthResVO implements Serializable {
    private static final long serialVersionUID = -37450800993043815L;

    private String url;
    private String authStatus;
    private String authNo;
}
