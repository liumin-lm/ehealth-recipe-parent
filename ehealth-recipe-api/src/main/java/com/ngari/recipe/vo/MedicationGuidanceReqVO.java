package com.ngari.recipe.vo;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @description： 用药指导入参
 * @author： ys
 * @date： 2021-07-21 15:03
 */
@Data
public class MedicationGuidanceReqVO implements Serializable {
    private static final long serialVersionUID = 4791717910521845139L;

    /**
     * 患者mpiId
     */
    private String mpiId;
    /**
     * 患者病例号
     */
    private String patientID;
    /**
     * 就诊科室名称
     */
    private String deptName;
    /**
     * 就诊号
     */
    private String adminNo;
    /**
     * 标识字段（暂未定义）默认0
     */
    private Integer flag;
    /**
     * 处方开具时间 精确到秒
     */
    private String createDate;

    @ItemProperty(alias = "开方机构")
    private Integer clinicOrgan;

    @ItemProperty(alias = "开方机构名称")
    private String organName;

    @ItemProperty(alias = "机构疾病名称")
    private String organDiseaseName;

    @ItemProperty(alias = "机构疾病编码")
    private String organDiseaseId;

    /**
     * 处方药品详情
     */
    private List<MedicationRecipeDetailVO> recipeDetails;

    /**
     * 请求类型（1：二维码扫码推送详情 2：自动推送详情链接跳转请求）
     */
    private Integer reqType;
}
