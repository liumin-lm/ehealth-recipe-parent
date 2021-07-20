package com.ngari.recipe.recipe.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 门诊处方
 * @author yinsheng
 * @date 2021\7\20 0020 08:29
 */
@Getter
@Setter
@NoArgsConstructor
public class OutPatientRecipeVO implements Serializable{
    private static final long serialVersionUID = -4388276725830720331L;

    /**
     * 挂号序号
     */
    private String registeredId;

    /**
     * 患者唯一ID
     */
    private String mpiId;

    /**
     * 患者姓名
     */
    private String patientName;

    /**
     * 患者性别
     */
    private String patientSex;
    /**
     * 处方类型
     */
    private Integer recipeType;

    /**
     * 院内处方号
     */
    private String recipeCode;

    /**
     * 患者病历号
     */
    private String patientId;

    /**
     * 处方类型文本
     */
    private String recipeTypeText;

    /**
     * 开方日期
     */
    private Date createDate;

    /**
     * 挂号科室代码
     */
    private String appointDepartCode;

    /**
     * 挂号科室名称
     */
    private String appointDepartName;

    /**
     * 开方医生代码
     */
    private String jobNumber;

    /**
     * 开方医生名称
     */
    private String doctorName;

    /**
     * 机构代码
     */
    private String organCode;

    /**
     * 机构名称
     */
    private String organName;

    /**
     * 配送方式 0 到院取药 1 医院配送 2 药企配送
     */
    private Integer giveMode;

    /**
     * 诊断代码
     */
    private String diseaseCode;

    /**
     * 诊断名称
     */
    private String diseaseName;

    /**
     * 当前处方状态
     */
    private Integer status;

    /**
     * 状态文本
     */
    private String statusText;

    /**
     * 长处方标志 0 普通处方 1 长处方
     */
    private Integer longRecipeFlag;

    /**
     * 延伸处方标志
     */
    private Integer extendRecipeFlag;

    /**
     * 总金额
     */
    private Double totalMoney;

    /**
     * 处方嘱托
     */
    private String recipeMemo;

    /**
     * 门诊处方项目药品明细
     */
    private List<OutPatientRecipeDetailVO> outPatientRecipeDetails;

}
