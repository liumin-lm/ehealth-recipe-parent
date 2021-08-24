package com.ngari.recipe.vo;

import ctd.schema.annotation.ItemProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 项目列表
 * @author yinsheng
 * @date 2021\8\21 0021 09:24
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItemListVO implements Serializable{
    private static final long serialVersionUID = 8715047453386510666L;

    @ItemProperty(alias = "项目id")
    private Integer id;
    @ItemProperty(alias = "机构id")
    private Integer organID;
    @ItemProperty(alias = "项目名称")
    private String itemName;
    @ItemProperty(alias = "项目编码")
    private String itemCode;
    @ItemProperty(alias = "项目单位")
    private String itemUnit;
    @ItemProperty(alias = "项目费用")
    private BigDecimal itemPrice;
    @ItemProperty(alias = "项目状态")
    private Integer status;
    private Integer start;
    private Integer limit;
}
