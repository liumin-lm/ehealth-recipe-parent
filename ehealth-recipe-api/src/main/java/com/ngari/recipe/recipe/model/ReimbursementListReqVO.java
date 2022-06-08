package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 查询报销清单列表入参
 * @author zgy
 * @date 2022/6/7 11:01
 */
@Data
public class ReimbursementListReqVO implements Serializable {

    private static final long serialVersionUID = -1150659176567206098L;

    @ItemProperty(alias="机构ID")
    private Integer organId;

    @ItemProperty(alias="患者唯一标识")
    private String mpiId;

    @ItemProperty(alias="查询开始时间")
    private Date startTime;

    @ItemProperty(alias="查询结束时间")
    private Date endTime;
}
