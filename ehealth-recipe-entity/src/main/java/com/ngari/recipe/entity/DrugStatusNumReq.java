package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author zgy
 * @date 2022/6/27 16:19
 */
@Data
public class DrugStatusNumReq implements Serializable {

    @ItemProperty(alias = "机构Id")
    private Integer organId;
}
