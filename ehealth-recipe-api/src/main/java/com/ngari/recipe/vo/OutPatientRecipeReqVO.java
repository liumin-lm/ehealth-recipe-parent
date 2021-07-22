package com.ngari.recipe.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 门诊处方查询入参
 * @author yinsheng
 * @date 2021\7\16 0016 14:16
 */
@Getter
@Setter
public class OutPatientRecipeReqVO implements Serializable{
    private static final long serialVersionUID = -1110551683869187915L;
    /**
     * 机构ID
     */
    private Integer organId;

    /**
     * 患者唯一号
     */
    private String mpiId;
    /**
     * 患者姓名
     */
    private String patientName;

    /**
     * 身份证号
     */
    private String IdCard;

    /**
     * 就诊卡号
     */
    private String cardID;

    /**
     * 查询开始时间
     */
    private String beginTime;

    /**
     * 查询结束时间
     */
    private String endTime;
}
