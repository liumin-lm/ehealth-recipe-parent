package com.ngari.recipe.dto;

import lombok.Data;

@Data
public class DrugInfoDTO {
    /**
     * 医疗机构代码
     */
    private Integer organId;
    /**
     * 药品序号
     */
    private Integer drugId;
    /**
     * 机构药品编码
     */
    private String organDrugCode;
    /**
     * 通用名
     */
    private String drugName;

    /**
     * 生产厂家代码
     */
    private String producerCode;
    /**
     * 药房
     */
    private String pharmacy;
    private String pharmacyCode;
    /**
     * 库存数量
     */
    private int stockAmount;
    /**
     * 库存数量中文
     */
    private String stockAmountChin;

    /**
     * 是否有库存 true：有 ，F：无
     */
    private Boolean stock;

    /**
     * 药房
     */
    private Integer pharmacyId;
    /**
     * 药品类型
     */
    private Integer drugType;
    /**
     * 药品适用业务 历史数据默认 1    1-药品处方 2-诊疗处方  保存方式类似  1,2
     */
    private String applyBusiness;
}
