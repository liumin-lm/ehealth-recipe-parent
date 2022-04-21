package com.ngari.recipe.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class HospitalDrugListReqVO implements Serializable {
    private static final long serialVersionUID = -6427526106718411107L;

    private Integer organId;
    private String drugName;
    private Integer pageNo;
}
