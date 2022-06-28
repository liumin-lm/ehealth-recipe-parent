package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author zgy
 * @date 2022/6/28 11:05
 */
@Data
public class AdvanceWarningReqVO implements Serializable {

    @ItemProperty(alias = "机构Id")
    private Integer organId;

    @ItemProperty(alias = "患者Id")
    private Integer patientId;

    @ItemProperty(alias = "患者姓名")
    private Integer patientName;

    @ItemProperty(alias = "患者唯一标识")
    private Integer mpiId;

    @ItemProperty(alias = "端标识符 0：PC 1：App")
    private Integer serverFlag;

    @ItemProperty(alias = "业务类型 0：处方 1：复诊 2：检验检查")
    private Integer businessType;


}
