package com.ngari.recipe.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 院内门诊返回结果
 * 组装给前端返回得字段
 * @author zgy
 * @date 2022/1/11 10:55
 */
@Data
public class WriteDrugRecipeBean implements Serializable {

    private static final long serialVersionUID = 8891378081107381886L;

    //挂号科室对应的行政科室id
    private Integer appointDepartInDepartId;
}
