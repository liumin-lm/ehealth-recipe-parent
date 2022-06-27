package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 机构药品目录药品操作状态
 * @author zgy
 * @date 2022/6/27 10:41
 */
@Data
public class DrugStatusNum implements Serializable {

    @ItemProperty(alias = "新增条数")
    private Long addStatusNum;

    @ItemProperty(alias = "更新条数")
    private Long updateStatusNum;
}
