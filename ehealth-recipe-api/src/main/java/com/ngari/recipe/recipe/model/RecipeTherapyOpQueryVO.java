package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 运营平台诊疗处方查询入参
 * @author zgy
 * @date 2021/12/31 15:41
 */
@Getter
@Setter
public class RecipeTherapyOpQueryVO implements Serializable {
    private static final long serialVersionUID = -3550249507091723787L;

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
    private String status;

    private int start;
    private int limit;
}
