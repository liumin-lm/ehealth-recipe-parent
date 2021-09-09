package com.ngari.recipe.dto;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.util.Date;

/**
 * @Author liumin
 * @Date 2021/8/20 下午3:19
 * @Description 处方撤销
 */
@Data
public class RecipeCancel {
    @ItemProperty(alias = "原因")
    private String cancelReason;

    @ItemProperty(alias = "时间")
    private Date cancelDate;
}
