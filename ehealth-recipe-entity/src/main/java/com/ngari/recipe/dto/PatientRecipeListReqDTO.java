package com.ngari.recipe.dto;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
    private Integer state;


    public static List<Integer> hisState(Integer state) {
        switch (state) {
            case 0:
                return Arrays.asList(1, 2, 3);
            case 2:
                return Collections.singletonList(1);
            case 4:
                return Arrays.asList(2, 3);
            default:
                return Collections.emptyList();
        }
    }
}
