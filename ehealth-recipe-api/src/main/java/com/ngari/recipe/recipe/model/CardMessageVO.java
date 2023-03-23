package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 卡片消息入参
 */
@Data
public class CardMessageVO implements Serializable {
    private static final long serialVersionUID = -6323400975911178581L;

    @ItemProperty(alias = "快捷购药")
    private List<Integer> fastRecipeIds;
    @ItemProperty(alias = "医生ID")
    private Integer doctorId;
    @ItemProperty(alias = "业务类型")
    private Integer bussSource;
    @ItemProperty(alias = "业务ID")
    private Integer clinicId;
}
