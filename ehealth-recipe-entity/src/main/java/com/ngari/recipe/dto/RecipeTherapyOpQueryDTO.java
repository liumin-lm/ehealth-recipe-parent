package com.ngari.recipe.dto;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * @author zgy
 * @date 2022/1/4 9:53
 */
@Getter
@Setter
public class RecipeTherapyOpQueryDTO implements Serializable {
    private static final long serialVersionUID = 5924319256660023000L;

    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "开始时间")
    private Date bDate;

    @ItemProperty(alias = "结束时间")
    private Date eDate;

    @ItemProperty(alias = "医生姓名/科室名称")
    private String doctorInfoSearch;

    @ItemProperty(alias = "主索引（患者编号）")
    private String mpiId;

    @ItemProperty(alias = "状态")
    private Integer status;

    private int start;
    private int limit;
}
