package com.ngari.recipe.dto;


import ctd.schema.annotation.ItemProperty;
import lombok.Data;


import java.io.Serializable;

@Data
public class RequirementsForTakingDTO implements Serializable {
    @ItemProperty(
            alias = "id"
    )
    private Integer id;

    @ItemProperty(
            alias = "机构编码"
    )
    private Integer organId;

    @ItemProperty(
            alias = "编码"
    )
    private String code;

    @ItemProperty(
            alias = "名称"
    )
    private String text;

    @ItemProperty(
            alias = "排序"
    )
    private Integer sort;


    @ItemProperty(
            alias = "煎法"
    )
    private String decoctionwayId;
}
