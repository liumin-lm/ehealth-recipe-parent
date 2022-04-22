package com.ngari.recipe.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 电子病例明细 特殊参数对象
 *
 * @author fuzi
 */
@Setter
@Getter
public class EmrDetailValueDTO {
    /**
     * 参数名称
     */
    private String name;
    /**
     * 参数代码
     */
    private String code;

    /**
     * 类型 0：默认老数据无法区分类型， 1：西医诊断，2：中医诊断
     */
    private Integer type;
}
