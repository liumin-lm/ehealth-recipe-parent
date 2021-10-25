package com.ngari.recipe.dto;

import lombok.Data;

import java.util.List;

/**
 * @author fuzi
 */
@Data
public class EnterpriseStock {
    /**
     * 购药按钮
     */
    private List<GiveModeButtonDTO> giveModeButton;
    /**
     * 配送药企代码
     */
    private String deliveryCode;

    /**
     * 配送药企名称
     */
    private String deliveryName;
    /**
     *  0默认，1医院配送，2药企配送
     */
    private Integer deliveryType;
    /**
     * 是否有库存 true：有 ，F：无
     */
    private Boolean stock;
    /**
     * 提示药品名称
     */
    private List<String> drugName;


}
