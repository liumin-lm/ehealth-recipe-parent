package com.ngari.recipe.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2021\7\22 0022 13:49
 */
@Setter
@Getter
@NoArgsConstructor
public class PatientInfoVO implements Serializable{
    private static final long serialVersionUID = 4078882102450236241L;
    /**
     * 机构ID
     */
    private Integer organId;

    /**
     * 患者名称
     */
    private String patientName;

    /**
     * 挂号序号
     */
    private String registerID;

    /**
     * 病历号
     */
    private String patientId;

    /**
     * 患者唯一号
     */
    private String mpiId;

    /**
     * 复诊单号
     */
    private Integer clinicId;
}
