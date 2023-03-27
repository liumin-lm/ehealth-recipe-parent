package com.ngari.recipe.offlinetoonline.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author liumin
 * @Date 2021/1/26 上午11:42
 * @Description 线下转线上获取详情参数
 */
@Data
public class CheckForOrderBeforeResVo implements Serializable {
    private static final long serialVersionUID = -7727248592234567484L;

    @ItemProperty(alias = "缴费状态 0：未支付 2：已支付 不是2的话都按未支付走")
    private Integer payFlag;

    @ItemProperty(alias = "存在状态 0：不存在 1：存在")
    private Integer existFlag;

}


