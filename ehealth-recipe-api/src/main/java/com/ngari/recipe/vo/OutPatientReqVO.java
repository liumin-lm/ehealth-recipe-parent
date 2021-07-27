package com.ngari.recipe.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class OutPatientReqVO implements Serializable{
    private static final long serialVersionUID = 5687117526863270649L;

    /**
     * 机构ID
     */
    private Integer organId;

    /**
     * 患者唯一号
     */
    private String mpiId;

    /**
     * 身份证号
     */
    private String IdCard;

    /**
     * 就诊卡号
     */
    private String cardID;
}
