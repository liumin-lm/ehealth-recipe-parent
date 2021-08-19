package com.ngari.recipe.dto;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.util.List;

/**
 * 电子病历 字段对象
 *
 * @author fuzi
 */
@Data
public class EmrDetailDTO {
    @ItemProperty(alias = "主诉")
    private String mainDieaseDescribe;

    @ItemProperty(alias = "现病史")
    private String currentMedical;

    @ItemProperty(alias = "既往史")
    private String histroyMedical;

    @ItemProperty(alias = "现病史")
    private String historyOfPresentIllness;

    @ItemProperty(alias = "过敏史")
    private String allergyMedical;

    @ItemProperty(alias = "体格检查")
    private String physicalCheck;

    @ItemProperty(alias = "处理方法")
    private String handleMethod;

    @ItemProperty(alias = "诊断备注")
    private String memo;

    @ItemProperty(alias = "机构疾病名称")
    private String organDiseaseName;
    @ItemProperty(alias = "机构疾病编码")
    private String organDiseaseId;

    private List<EmrDetailValueDTO> diseaseValue;

    @ItemProperty(alias = "中医症候编码")
    private String symptomId;
    @ItemProperty(alias = "中医症候名称")
    private String symptomName;

    private List<EmrDetailValueDTO> symptomValue;

}
