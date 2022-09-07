package com.ngari.recipe.recipe.model;

import com.ngari.patient.dto.UsePathwaysDTO;
import com.ngari.patient.dto.UsingRateDTO;
import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Description
 * @Author lium
 * @Date 2022-08-04
 */
@Data
public class RequirementsForTakingVO implements Serializable {
    private static final long serialVersionUID = -7257665728902493423L;

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
