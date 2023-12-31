package com.ngari.recipe.dto;

import com.ngari.recipe.entity.DrugsEnterprise;
import lombok.Data;

import java.util.List;

/**
 * 药企购药配置-库存对象
 *
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
    private Integer appointEnterpriseType;
    /**
     * 是否有库存 true：有 ，F：无
     */
    private Boolean stock;
    /**
     * 提示药品名称
     */
    private List<String> drugName;
    /**
     * 药企id
     */
    private Integer drugsEnterpriseId;
    /**
     * 药企对象
     */
    private DrugsEnterprise drugsEnterprise;

    /**
     * 库存药品信息
     */
    private List<DrugInfoDTO> drugInfoList;

    /**
     * 有库存药品id
     */
    private List<Integer> drugIds;

    /**
     * 药企是否支持配送标识
     */
    private Boolean sendFlag = true;

}
