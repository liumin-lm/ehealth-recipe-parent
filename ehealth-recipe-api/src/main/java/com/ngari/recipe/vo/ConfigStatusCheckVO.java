package com.ngari.recipe.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author fuzi
 */
@Getter
@Setter
public class ConfigStatusCheckVO implements Serializable {
    private static final long serialVersionUID = 223429953403953693L;
    /**
     * 主键
     */
    private Integer id;
    /**
     * 位置：1处方状态，2订单状态（院取药订），3订单状态（配送到家）...
     */
    private Integer location;
    /**
     * 位置备注
     */
    private String locationRemark;
    /**
     * 源状态
     */
    private Integer source;
    /**
     * 源名称
     */
    private String sourceName;
    /**
     * 目标状态
     */
    private Integer target;
    /**
     * 目标名称
     */
    private String targetName;
    /**
     * 备注
     */
    private String remark;
}
