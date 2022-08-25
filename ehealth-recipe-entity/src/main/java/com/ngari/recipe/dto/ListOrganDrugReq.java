package com.ngari.recipe.dto;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

/**
 * 获取机构药品目录入参
 *
 * @author lium
 */
@Data
public class ListOrganDrugReq implements java.io.Serializable {
    private static final long serialVersionUID = -2026791423853766129L;

    @ItemProperty(alias = "机构")
    private Integer organId;

    @ItemProperty(alias = "页码")
    private Integer page;

    @ItemProperty(alias = "默认500条")
    private Integer limit;

    @ItemProperty(alias = "开始时间")
    private String startDate;

    @ItemProperty(alias = "结束时间")
    private String endDate;


}
