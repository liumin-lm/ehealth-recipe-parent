package com.ngari.recipe.dto;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @description： 患者端 处方列表请求入参
 * @author： whf
 * @date： 2023-03-01 9:43
 */
@Data
public class PatientRecipeListReqDTO implements Serializable {

    private static final long serialVersionUID = -5399916369848862808L;
    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "用户 mpiId")
    private String mpiId;

    @ItemProperty(alias = "开始时间")
    private Date startTime;

    @ItemProperty(alias = "结束时间")
    private Date endTime;

    @ItemProperty(alias = "0 全部 1 待审方 2 待缴费 3 收取中 4 已结束")
    private String state;
}
