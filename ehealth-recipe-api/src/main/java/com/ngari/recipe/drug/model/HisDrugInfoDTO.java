package com.ngari.recipe.drug.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author  Created by liuxiaofeng on 2020/12/16.
 * 搜索his药品返回实体
 */
@Data
public class HisDrugInfoDTO implements Serializable{
    private static final long serialVersionUID = 8025324925285525443L;

    @ItemProperty(alias = "是否有下一页")
    private Boolean hasNextPage;
    @ItemProperty(alias = "下一页页码")
    private Integer nextPage;
    @ItemProperty(alias = "药品信息列表")
    private List<SearchDrugDetailDTO> drugDetailList;
}
