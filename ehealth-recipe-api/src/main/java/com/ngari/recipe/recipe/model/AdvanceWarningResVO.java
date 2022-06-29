package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author zgy
 * @date 2022/6/28 10:15
 */
@Data
public class AdvanceWarningResVO implements Serializable {

    @ItemProperty(alias = "医疗保障职能监管子系统回显hisUrl弹窗地址")
    private String popUrl;
}
