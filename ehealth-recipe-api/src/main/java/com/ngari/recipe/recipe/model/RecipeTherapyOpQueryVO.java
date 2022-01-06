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

    @ItemProperty(alias = "机构id列表")
    private List<Integer> organIds;

    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "开始时间")
    private Date bDate;

    @ItemProperty(alias = "结束时间")
    private Date eDate;

    @ItemProperty(alias = "患者/医生/科室名称")
    private String searchName;

    /*@ItemProperty(alias = "挂号科室名称")
    private String appointDepartName;

    @ItemProperty(alias = "患者姓名")
    private String patientName;

    @ItemProperty(alias = "医生姓名")
    private String doctorName;*/

    @ItemProperty(alias = "患者电话")
    private String patientMobile;

    @ItemProperty(alias = "状态")
    private String status;

    private int start;
    private int limit;
}
