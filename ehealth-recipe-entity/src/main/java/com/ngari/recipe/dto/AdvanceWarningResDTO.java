package com.ngari.recipe.dto;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author zgy
 * @date 2022/6/28 11:10
 */
@Data
public class AdvanceWarningResDTO implements Serializable {

    @ItemProperty(alias = "医疗保障职能监管子系统回显hisUrl弹窗地址")
    private String popUrl;
}
