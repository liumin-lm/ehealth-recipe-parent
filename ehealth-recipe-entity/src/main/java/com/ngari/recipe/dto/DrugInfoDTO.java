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
}
