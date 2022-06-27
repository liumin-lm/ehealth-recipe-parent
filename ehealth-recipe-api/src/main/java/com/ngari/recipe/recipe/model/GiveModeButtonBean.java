package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2020\12\3 0003 13:49
 */
@Data
public class GiveModeButtonBean implements Serializable {

    private static final long serialVersionUID = -5939365788332205225L;

    @ItemProperty(alias = "按钮key")
    private String showButtonKey;

    @ItemProperty(alias = "展示前端按钮名称")
    private String showButtonName;

    @ItemProperty(alias = "对接方式：  1:标准, 2:门诊缴费, 3:跳转到第三方, 4:小程序")
    private String buttonSkipType;

    @ItemProperty(alias = "小程序appId")
    private String appId;

    @ItemProperty(alias = "跳转链接，目前对接方式为第三方和小程序时配置")
    private String skipUrl;

}
