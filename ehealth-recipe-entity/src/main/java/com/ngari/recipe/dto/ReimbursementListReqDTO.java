package com.ngari.recipe.dto;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author zgy
 * @date 2022/6/9 9:41
 */
@Data
public class ReimbursementListReqDTO implements Serializable {
    private static final long serialVersionUID = 5019216757490269106L;

    @ItemProperty(alias="机构ID")
    private Integer organId;

    @ItemProperty(alias="患者唯一标识")
    private String mpiId;

    @ItemProperty(alias="查询开始时间")
    private Date startTime;

    @ItemProperty(alias="查询结束时间")
    private Date endTime;
}
