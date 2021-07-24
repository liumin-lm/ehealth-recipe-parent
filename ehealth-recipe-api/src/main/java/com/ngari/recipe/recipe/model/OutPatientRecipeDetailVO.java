package com.ngari.recipe.recipe.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 门诊处方项目药品明细
 * @author yinsheng
 * @date 2021\7\20 0020 09:27
 */
@Setter
@Getter
@NoArgsConstructor
public class OutPatientRecipeDetailVO implements Serializable{
    private static final long serialVersionUID = -2410605425201952548L;

    /**
     * 机构药品编码
     */
    private String organDrugCode;
    /**
     * 药品标识 1 药品 2 项目
     */
    private Integer drugFlag;

    /**
     * 药品名称
     */
    private String drugName;

    /**
     * 药品规格
     */
    private String drugSpec;

    /**
     * 药品剂型
     */
    private String drugForm;

    /**
     * 用药频率
     */
    private String usingRate;

    /**
     * 用药频率说明
     */
    private String usingRateText;

    /**
     * 用药方式
     */
    private String usePathways;

    /**
     * 用药方式说明
     */
    private String usePathwaysText;

    /**
     * 药品剂量
     */
    private String useDose;

    /**
     * 剂量单位
     */
    private String useDoseUnit;

    /**
     * 药品单位
     */
    private String drugUnit;

    /**
     * 数量
     */
    private String useTotalDose;

    /**
     * 单价
     */
    private String price;

    /**
     * 总金额
     */
    private String totalPrice;

    /**
     * 药品备注说明
     */
    private String memo;

}
