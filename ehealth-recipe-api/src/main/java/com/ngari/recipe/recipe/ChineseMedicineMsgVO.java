package com.ngari.recipe.recipe;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Description
 * @Author yzl
 * @Date 2022-06-06
 */
@Data
@Schema
public class ChineseMedicineMsgVO implements Serializable {

    private static final long serialVersionUID = 8028608699175646782L;

    @ItemProperty(alias = "剂数, 帖数")
    private Integer copyNum;

    @ItemProperty(alias = "总费用")
    private BigDecimal totalFee;

    @ItemProperty(alias = "制法")
    private String makeMethodText;

    @ItemProperty(alias = "煎法")
    private String decoctionText;

    @ItemProperty(alias = "每付取汁量")
    private String juice;

    @ItemProperty(alias = "用法")
    @Dictionary(id = "eh.cdr.dictionary.UsePathways")
    private String usePathways;

    @ItemProperty(alias = "频次")
    @Dictionary(id = "eh.cdr.dictionary.UsingRate")
    private String usingRate;

    @ItemProperty(alias = "每次用量")
    private String minor;

    @ItemProperty(alias = "天数")
    private Integer useDays;

    @ItemProperty(alias = "嘱托")
    private String memo;

    @ItemProperty(alias = "是否代煎")
    private Boolean decoctionFlag;

}
