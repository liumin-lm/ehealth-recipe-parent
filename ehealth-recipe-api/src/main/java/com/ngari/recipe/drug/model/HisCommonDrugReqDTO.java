package com.ngari.recipe.drug.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author Created by liuxiaofeng on 2020/12/15.
 *         查询his常用药列表入参实体
 */
@Data
public class HisCommonDrugReqDTO implements Serializable {
    private static final long serialVersionUID = 5075888382766133L;

    @ItemProperty(alias = "机构id")
    private Integer organId;
    @ItemProperty(alias = "机构名称")
    private String organName;
    @ItemProperty(alias = "商保渠道编码")
    private String lineCode;
    @ItemProperty(alias = "药品分类编码")
    private Integer drugType;
    @ItemProperty(alias = "医生id")
    private Integer doctorId;
    @ItemProperty(alias = "医生名称")
    private String doctorName;
    @ItemProperty(alias = "科室编码")
    private String deptCode;
    @ItemProperty(alias = "科室名称")
    private String deptName;
    @ItemProperty(alias = "常用药品数量")
    private Integer drugLimit;
    @ItemProperty(alias = "患者id")
    private String mpiId;
    @ItemProperty(alias = "诊断信息")
    private List<PatientDiagnosisDTO> diagnosisList;


}
