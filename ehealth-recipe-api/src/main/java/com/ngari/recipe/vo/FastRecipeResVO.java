package com.ngari.recipe.vo;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
public class FastRecipeResVO implements Serializable {
    private static final long serialVersionUID = 3181309499804281168L;

    @ItemProperty(alias = "最后需支付费用")
    private BigDecimal actualPrice;

    @ItemProperty(alias = "诊断备注")
    private String memo;

    @ItemProperty(alias = "开方机构")
    private Integer clinicOrgan;

    @ItemProperty(alias = "开方机构名称")
    private String organName;

    @ItemProperty(alias = "处方类型 1 西药 2 中成药")
    private Integer recipeType;

    @ItemProperty(alias = "剂数")
    private Integer copyNum;

    @ItemProperty(alias = "处方金额")
    private BigDecimal totalMoney;

    @ItemProperty(alias = "发药方式")
    private Integer giveMode;

    @ItemProperty(alias = "来源标志")
    private Integer fromflag;

    private FastRecipeExtend recipeExtend;
}
