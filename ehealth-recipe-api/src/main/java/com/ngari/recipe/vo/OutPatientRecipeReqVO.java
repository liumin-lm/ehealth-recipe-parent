package com.ngari.recipe.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 门诊处方查询入参
 * @author yinsheng
 * @date 2021\7\16 0016 14:16
 */
@Getter
@Setter
@NoArgsConstructor
public class OutPatientRecipeReqVO extends OutPatientReqVO implements Serializable{
    private static final long serialVersionUID = -1110551683869187915L;

    /**
     * 机构名称
     */
    private String organName;
    /**
     * 患者姓名
     */
    private String patientName;

    /**
     * 患者病历号
     */
    private String patientId;

    /**
     * 查询开始时间
     */
    private String beginTime;

    /**
     * 查询结束时间
     */
    private String endTime;
}
