package com.ngari.recipe.drug.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author  Created by liuxiaofeng on 2020/12/16.
 * 患者诊断信息入参
 */
@Data
public class PatientDiagnosisDTO implements Serializable{
    private static final long serialVersionUID = -7515486666078282910L;

    @ItemProperty(alias = "诊断编码")
    private String diagnosisCode;
    @ItemProperty(alias = "诊断名称")
    private String diagnosisName;
}
